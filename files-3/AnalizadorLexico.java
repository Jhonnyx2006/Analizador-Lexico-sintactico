package compilador;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class AnalizadorLexico {

    private final String texto;
    private int posicion = 0, linea = 1, columna = 1;

    private final List<Token> tokens           = new ArrayList<>();
    private final List<ErrorLexico> errores    = new ArrayList<>();
    private final List<Token> tablaSimbolos    = new ArrayList<>();


  
    // CAMBIO: la tabla de palabras reservadas (texto -> clasificacion lexica entera) esta en ClasificacionLexica
    private static final List<Character> CARACTERES_SIMPLES = Arrays.asList(
        ';', '=', '+', '-', '/', '(', ')', ',', '<', '>', '*'
    );

    public AnalizadorLexico(String texto) { this.texto = texto; }

    public static void main(String[] args) {
        String rutaArchivo = "programaEjemplo.txt";   // ruta relativa
        String codigo = "";

        System.out.printf("Abriendo el archivo '%s'...%n", rutaArchivo);
        try {
            codigo = Files.readString(Paths.get(rutaArchivo));
        } catch (IOException e) {
            System.err.println("No se pudo leer el archivo '" + rutaArchivo + "': " + e.getMessage());
            return;
        }
        System.out.printf("Archivo leido correctamente: %d caracteres.%n%n", codigo.length());

        AnalizadorLexico lex = new AnalizadorLexico(codigo);

        System.out.println("_____ TRAZA DE EJECUCIoN _____");
        lex.analizar();
        System.out.println("\n_____ FIN DE LA TRAZA _____\n");

        lex.imprimir();
    }

    // CAMBIO: antes analizar() recorria todo el texto de una vez. Ahora el trabajo esta en siguienteToken(),
    // que forma UN token y regresa; analizar() solo lo llama hasta llegar al fin del archivo.
    public void analizar() {
        Token t;
        do {
            t = siguienteToken();
        } while (t.getClasificacion() != ClasificacionLexica.FIN_ARCHIVO);
    }

    // CAMBIO: es el "scanner()" que llama el analizador sintactico cada vez que necesita un token.
    // Lee caracter por caracter desde donde se quedo (posicion) hasta formar un token, y lo regresa.
    public Token siguienteToken() {
        int tokensAntes = tokens.size();

        while (posicion < texto.length()) {
            char c = texto.charAt(posicion);

            //si es un blanco me hace ir al siguiente caracter
            if (esBlanco(c)) {
                System.out.printf("[L%d:C%d] Se lee %s -> es un blanco, lo ignoro y paso al siguiente%n",
                        linea, columna, visible(c));
                avanzar();
                if (c == '\n') {
                    System.out.printf("         (salto de linea: ahora estoy en L%d:C%d)%n", linea, columna);
                }
                continue;
            }

            if (esLetra(c) || c == '_') {
                System.out.printf("[L%d:C%d] Se lee '%c' -> es letra o '_': empiezo a formar un identificador / palabra reservada%n",
                        linea, columna, c);
                analizarIdentificadorOPalabraReservada();
            } else if (esDigito(c)) {
                System.out.printf("[L%d:C%d] Se lee '%c' -> es un digito: empiezo a formar un numero%n",
                        linea, columna, c);
                analizarNumero();
            } else if (CARACTERES_SIMPLES.contains(c)) {
                System.out.printf("[L%d:C%d] Se lee '%c' -> esta en el conjunto de caracteres simples%n",
                        linea, columna, c);
                analizarCaracterSimple();
            } else {
                reportarError(c); //aqui van los caracteres no reconocidos
            }

            // si se formo un token se entrega; si fue un error lexico se sigue leyendo
            if (tokens.size() > tokensAntes) {
                return tokens.get(tokens.size() - 1);
            }
        }
        System.out.println("[FIN] Ya no quedan caracteres por leer.");
        return new Token(ClasificacionLexica.FIN_ARCHIVO, "$", linea, columna);
    }

    private void analizarIdentificadorOPalabraReservada() {
        int l = linea, col = columna;
        String lexema = "";

        while (posicion < texto.length()) {
            char c = texto.charAt(posicion);
            if (esLetra(c) || esDigito(c) || c == '_') {
                System.out.printf("    Se lee '%c' (L%d:C%d) -> es letra, digito o '_': lo agrego al lexema%n",
                        c, linea, columna);
                lexema += c;
                System.out.printf("    Lexema parcial: \"%s\"%n", lexema);
                avanzar();
            } else break;
        }
        trazarFinLexema();
        System.out.printf("    Lexema completo formado: \"%s\" (empieza en L%d:C%d)%n", lexema, l, col);

        // CAMBIO: se busca en la tabla de palabras reservadas y se obtiene su clasificacion lexica (un entero)
        int clasificacionPR = ClasificacionLexica.codigoPalabraReservada(lexema);
        if (clasificacionPR != ClasificacionLexica.NO_RESERVADA) {
            System.out.printf("    \"%s\" esta en la tabla de palabras reservadas -> es una PALABRA RESERVADA, clasificacion lexica %d (%s)%n",
                    lexema, clasificacionPR, ClasificacionLexica.nombre(clasificacionPR));
            agregarToken(new Token(clasificacionPR, lexema, l, col));
            return;
        }
        System.out.printf("    \"%s\" no es palabra reservada, reviso si cumple las reglas de un identificador:%n", lexema);

        if (!esIdentificadorValido(lexema)) {
            System.out.printf("    \"%s\" NO es un identificador valido -> se registra un error%n", lexema);
            agregarError("Identificador invalido: '" + lexema + "'", l, col);
            return;
        }
        System.out.printf("    \"%s\" cumple todas las reglas -> es un IDENTIFICADOR, clasificacion lexica %d%n",
                lexema, ClasificacionLexica.IDENTIFICADOR);

        // si no estaba en la tabla de palabras reservadas y cumple las reglas, es un identificador
        agregarToken(new Token(ClasificacionLexica.IDENTIFICADOR, lexema, l, col));
    }


    private boolean esIdentificadorValido(String s) {
        if (s.length() == 0) {
            System.out.println("      Regla: el lexema esta vacio -> no cumple");
            return false;
        }

        // CAMBIO: antes se hacia s.charAt(s.length() - 1) para ir directo al ultimo caracter, lo cual un
        // analizador real no puede hacer. Ahora se recorre de izquierda a derecha, un caracter a la vez,
        // y solo se recuerdan tres cosas: el primero, el ultimo leido hasta el momento y si todos fueron validos.
        char primero = ' ';
        char ultimo  = ' ';
        boolean todosValidos = true;
        int posInvalida = -1;
        char caracterInvalido = ' ';

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);                       // el caracter que toca leer ahora
            if (i == 0) primero = c;                    // el primero se guarda al leerlo
            ultimo = c;                                 // el ultimo es simplemente el ultimo que se ha leido

            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok && todosValidos) {                  // se recuerda el primer caracter invalido
                todosValidos = false;
                posInvalida = i;
                caracterInvalido = c;
            }
        }
        System.out.printf("      Primer caracter: '%c' | ultimo caracter: '%c'%n", primero, ultimo);

        if (primero == ultimo) {                                                      // es para identificadores unicos : x
            System.out.printf("      Regla 1: el primer y el ultimo caracter son iguales ('%c') -> NO cumple%n", primero);
            return false;
        }
        System.out.println("      Regla 1: el primer y el ultimo caracter son distintos -> cumple");

        if (!(primero >= 'a' && primero <= 'z')) {                                    // inicio
            System.out.printf("      Regla 2: debe iniciar con letra minuscula y inicia con '%c' -> NO cumple%n", primero);
            return false;
        }
        System.out.printf("      Regla 2: inicia con letra minuscula ('%c') -> cumple%n", primero);

        if (!((ultimo >= 'a' && ultimo <= 'z') || (ultimo >= '0' && ultimo <= '9'))) { // fin
            System.out.printf("      Regla 3: debe terminar en minuscula o digito y termina en '%c' -> NO cumple%n", ultimo);
            return false;
        }
        System.out.printf("      Regla 3: termina en minuscula o digito ('%c') -> cumple%n", ultimo);

        if (!todosValidos) {                                                          // intermedios
            System.out.printf("      Regla 4: el caracter '%c' (posicion %d) no es minuscula, digito ni '_' -> NO cumple%n", caracterInvalido, posInvalida);
            return false;
        }
        System.out.println("      Regla 4: todos los caracteres son minusculas, digitos o '_' -> cumple");
        return true;
    }


    private void analizarNumero() {
        int l = linea, col = columna;
        String lexema = "";
        boolean esFlotante = false;

        while (posicion < texto.length() && esDigito(texto.charAt(posicion))) {
            System.out.printf("    Se lee '%c' (L%d:C%d) -> es digito, lo agrego a la parte entera%n",
                    texto.charAt(posicion), linea, columna);
            lexema += texto.charAt(posicion);
            System.out.printf("    Lexema parcial: \"%s\"%n", lexema);
            avanzar();
        }

        if (posicion + 1 < texto.length()
                && texto.charAt(posicion) == '.'
                && esDigito(texto.charAt(posicion + 1))) {
            System.out.printf("    Se lee '.' (L%d:C%d) seguido del digito '%c' -> es un punto decimal, el numero es FLOTANTE%n",
                    linea, columna, texto.charAt(posicion + 1));
            esFlotante = true;
            lexema += '.';
            System.out.printf("    Lexema parcial: \"%s\"%n", lexema);
            avanzar();
            while (posicion < texto.length() && esDigito(texto.charAt(posicion))) {
                System.out.printf("    Se lee '%c' (L%d:C%d) -> es digito, lo agrego a la parte decimal%n",
                        texto.charAt(posicion), linea, columna);
                lexema += texto.charAt(posicion);
                System.out.printf("    Lexema parcial: \"%s\"%n", lexema);
                avanzar();
            }
            trazarFinLexema();
        } else {
            if (posicion < texto.length() && texto.charAt(posicion) == '.') {
                System.out.printf("    Se lee '.' (L%d:C%d) pero no le sigue un digito -> no es parte del numero, lo dejo para después%n",
                        linea, columna);
            } else {
                trazarFinLexema();
            }
        }
        System.out.printf("    Numero completo formado: \"%s\" (empieza en L%d:C%d)%n", lexema, l, col);


        if (!esFlotante && lexema.length() == 1 && lexema.charAt(0) == '0') {
            System.out.println("    El numero es exactamente '0' -> no se acepta, se registra un error");
            agregarError("Numero invalido: '0'", l, col);
            return; //esto es para que termine la funcion
        }

        int clasificacion = esFlotante
                ? ClasificacionLexica.LITERAL_REAL
                : ClasificacionLexica.LITERAL_ENTERA;

        System.out.printf("    \"%s\" es un numero %s -> es un %s, clasificacion lexica %d%n", lexema,
                esFlotante ? "con punto decimal" : "sin punto decimal",
                esFlotante ? "FLOTANTE" : "ENTERO", clasificacion);

        agregarToken(new Token(clasificacion, lexema, l, col));
    }

    private void analizarCaracterSimple() {
        int l = linea, col = columna;
        char c = texto.charAt(posicion);
        String lexema = "" + c;
        int clasificacion = c;   // CAMBIO: la clasificacion lexica de un caracter unico es su codigo ASCII
        avanzar();

        // foorma un operador de dos caracteres? (== o <>)
        if (posicion < texto.length()) {
            char siguiente = texto.charAt(posicion);
            System.out.printf("    Miro el siguiente caracter: %s (L%d:C%d) -> ¿forma un operador de dos caracteres?%n",
                    visible(siguiente), linea, columna);
            if (c == '=' && siguiente == '=') {
                System.out.println("    '=' seguido de '=' -> es el operador \"==\"");
                lexema = "==";
                clasificacion = ClasificacionLexica.OP_IGUAL_IGUAL;
                avanzar();
            } else if (c == '<' && siguiente == '>') {
                System.out.println("    '<' seguido de '>' -> es el operador \"<>\"");
                lexema = "<>";
                clasificacion = ClasificacionLexica.OP_DISTINTO;
                avanzar();
            } else {
                System.out.printf("    No forma operador doble -> el token es solo \"%s\"%n", lexema);
            }
        } else {
            System.out.printf("    Ya no hay mas caracteres -> el token es solo \"%s\"%n", lexema);
        }

        if (lexema.length() == 1) {
            System.out.printf("    Es un caracter unico -> su clasificacion lexica es su codigo ASCII: %d%n", clasificacion);
        } else {
            System.out.printf("    Es un operador de dos caracteres -> clasificacion lexica %d (%s)%n",
                    clasificacion, ClasificacionLexica.nombre(clasificacion));
        }

        agregarToken(new Token(clasificacion, lexema, l, col));
    }

    private void reportarError(char c) {
        System.out.printf("[L%d:C%d] Se lee %s -> no es letra, digito, blanco ni caracter simple: no pertenece al lenguaje%n",
                linea, columna, visible(c));
        agregarError("Caracter no reconocido: '" + c + "'", linea, columna);
        avanzar();
    }

    private void agregarToken(Token t) {
        System.out.printf("    >>> TOKEN generado: %s%n", t);
        tokens.add(t);
        if (t.getClasificacion() == ClasificacionLexica.IDENTIFICADOR) {
            if (estaEnTablaSimbolos(t.getNombrePropio())) {
                System.out.printf("    >>> '%s' ya estaba en la tabla de simbolos, no se vuelve a insertar%n", t.getNombrePropio());
            } else {
                System.out.printf("    >>> '%s' es nuevo: se inserta en la tabla de simbolos%n", t.getNombrePropio());
                tablaSimbolos.add(t); // solo se inserta si el nombre no existe
            }
        }
    }

    // es para cver si ya esta en la tabla 
    private boolean estaEnTablaSimbolos(String nombre) {
        for (Token s : tablaSimbolos) {
            if (s.getNombrePropio().equals(nombre)) return true;
        }
        return false;
    }

    private void agregarError(String mensaje, int l, int c) {
        ErrorLexico e = new ErrorLexico(mensaje, l, c);
        errores.add(e);
        System.out.printf("    >>> ERROR registrado: %s%n", e);
    }

    // sew termino el lexema  ? 
    private void trazarFinLexema() {
        if (posicion < texto.length()) {
            System.out.printf("    Se lee %s (L%d:C%d) -> ya no pertenece al lexema, termino de leerlo%n",
                    visible(texto.charAt(posicion)), linea, columna);
        } else {
            System.out.println("    Se llego al final del texto -> termino de leer el lexema");
        }
    }

    // es para que los cafracteres no leidos se vean en los logs 
    private static String visible(char c) {
        switch (c) {
            case ' ':  return "[espacio]";
            case '\t': return "[tabulador]";
            case '\n': return "[salto de linea]";
            case '\r': return "[retorno de carro]";
            default:   return "'" + c + "'";
        }
    }

    private void avanzar() {
        char c = texto.charAt(posicion++);
        if (c == '\n') { linea++; columna = 1; }
        else           { columna++; }
    }

    private static boolean esBlanco(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    } //regresa verdadero si es un blanco
    private static boolean esLetra(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    private static boolean esDigito(char c) {
        return c >= '0' && c <= '9';
    }

    public void imprimir() {
        System.out.println("_____ TOKENS ______");
        for (Token t : tokens) System.out.println("  " + t);

        System.out.println("\n___ TABLA DE SIMBOLOS _____");
        if (tablaSimbolos.isEmpty()) System.out.println("  (vacia)");
        else for (Token s : tablaSimbolos) System.out.printf("  %-15s -> %d (%s)%n", s.getNombrePropio(), s.getClasificacion(), ClasificacionLexica.nombre(s.getClasificacion()));
        //%-15s imprime el nombre propio y deja 15 espacios en blanco, %d imprime la clasificacion lexica (entero), %s su nombre, %n salto de linea
        System.out.println("\n____ ERRORES ____");
        if (errores.isEmpty()) System.out.println("  (ninguno)");
        else errores.forEach(e -> System.out.println("  " + e));
    }

    public static class Token {
        // CAMBIO: un token es el par (clasificacion lexica entera, nombre propio del lexema) + su posicion
        private final int clasificacion;
        private final String nombrePropio;
        private final int linea, columna;

        public Token(int clasificacion, String n, int l, int c) {
            this.clasificacion = clasificacion; nombrePropio = n; linea = l; columna = c;
        }
        public int getClasificacion()   { return clasificacion; }
        public String getNombrePropio() { return nombrePropio; }
        public int getLinea()           { return linea; }
        public int getColumna()         { return columna; }

        @Override public String toString() {
            return String.format("[%3d %-14s] '%s'  (L%d:C%d)", clasificacion, ClasificacionLexica.nombre(clasificacion), nombrePropio, linea, columna);
        } //formatea el texto
    }

    public static class ErrorLexico {
        private final String mensaje;
        private final int linea, columna;
        public ErrorLexico(String m, int l, int c) { mensaje = m; linea = l; columna = c; }
        @Override public String toString() {
            return String.format("Error (L%d:C%d): %s", linea, columna, mensaje);
        } //%d entero decimal
    }

    // CAMBIO: el enum ClasificacionLexica desaparecio. Las clasificaciones son enteros y estan en
    // ClasificacionLexica.java (0 = fin de archivo, 1..255 = ASCII del caracter unico, 256+ = demas lexemas)
}
