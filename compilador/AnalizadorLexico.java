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

        // System.out.printf("Abriendo el archivo '%s'...%n", rutaArchivo);
        try {
            codigo = Files.readString(Paths.get(rutaArchivo));
        } catch (IOException e) {
            System.err.println("No se pudo leer el archivo '" + rutaArchivo + "': " + e.getMessage());
            return;
        }

        AnalizadorLexico lex = new AnalizadorLexico(codigo);

        lex.analizar();

        lex.imprimir();
    }

    // CAMBIO: antes analizar() recorria todo el texto de una vez. Ahora el trabajo esta en siguienteToken(),
    // que forma UN token y regresa analizar() solo lo llama hasta llegar al fin del archivo.
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
            
                avanzar();
                if (c == '\n') {
                }
                continue;
            }

            if (esLetra(c) || c == '_') {
                analizarIdentificadorOPalabraReservada();
            } else if (esDigito(c)) {
                analizarNumero();
            } else if (CARACTERES_SIMPLES.contains(c)) {
                analizarCaracterSimple();
            } else {
                reportarError(c); //aqui van los caracteres no reconocidos
            }

            // si se formo un token se entrega; si fue un error lexico se sigue leyendo
            if (tokens.size() > tokensAntes) {
                return tokens.get(tokens.size() - 1);
            }
        }
        return new Token(ClasificacionLexica.FIN_ARCHIVO, "$", linea, columna);
    }

    private void analizarIdentificadorOPalabraReservada() {
        int l = linea, col = columna;
        String lexema = "";

        while (posicion < texto.length()) {
            char c = texto.charAt(posicion);
            if (esLetra(c) || esDigito(c) || c == '_') {
                lexema += c;
                avanzar();
            } else break;
        }
        trazarFinLexema();

        // CAMBIO: se busca en la tabla de palabras reservadas y se obtiene su clasificacion lexica (un entero)
        int clasificacionPR = ClasificacionLexica.codigoPalabraReservada(lexema);
        if (clasificacionPR != ClasificacionLexica.NO_RESERVADA) {
            agregarToken(new Token(clasificacionPR, lexema, l, col));
            return;
        }

        if (!esIdentificadorValido(lexema)) {
            agregarError("Identificador invalido: '" + lexema + "'", l, col);
            return;
        }

        // si no estaba en la tabla de palabras reservadas y cumple las reglas, es un identificador
        agregarToken(new Token(ClasificacionLexica.IDENTIFICADOR, lexema, l, col));
    }


    private boolean esIdentificadorValido(String s) {
        if (s.length() == 0) {
            // System.out.println("      Regla: el lexema esta vacio -> no cumple");
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

       

        if (!(primero >= 'a' && primero <= 'z')) {                                    // inicio
            // System.out.printf("      Regla 2: debe iniciar con letra minuscula y inicia con '%c' -> NO cumple%n", primero);
            return false;
        }

        if (!((ultimo >= 'a' && ultimo <= 'z') || (ultimo >= '0' && ultimo <= '9'))) { // fin
            // System.out.printf("      Regla 3: debe terminar en minuscula o digito y termina en '%c' -> NO cumple%n", ultimo);
            return false;
        }

        if (!todosValidos) {                                                          // intermedios
            return false;
        }
        return true;
    }


    private void analizarNumero() {
        int l = linea, col = columna;
        String lexema = "";
        boolean esFlotante = false;

        while (posicion < texto.length() && esDigito(texto.charAt(posicion))) {
            lexema += texto.charAt(posicion);
            avanzar();
        }

        if (posicion + 1 < texto.length()
                && texto.charAt(posicion) == '.'
                && esDigito(texto.charAt(posicion + 1))) {
            esFlotante = true;
            lexema += '.';
            // System.out.printf("    Lexema parcial: \"%s\"%n", lexema);
            avanzar();
            while (posicion < texto.length() && esDigito(texto.charAt(posicion))) {
                lexema += texto.charAt(posicion);
                // System.out.printf("    Lexema parcial: \"%s\"%n", lexema);
                avanzar();
            }
            trazarFinLexema();
        } else {
            if (posicion < texto.length() && texto.charAt(posicion) == '.') {
            } else {
                trazarFinLexema();
            }
        }


        if (!esFlotante && lexema.length() == 1 && lexema.charAt(0) == '0') {
            agregarError("Numero invalido: '0'", l, col);
            return; //esto es para que termine la funcion
        }

        int clasificacion = esFlotante
                ? ClasificacionLexica.LITERAL_REAL
                : ClasificacionLexica.LITERAL_ENTERA;

        // System.out.printf("    \"%s\" es un numero %s -> es un %s, clasificacion lexica %d%n", lexema,
        //         esFlotante ? "con punto decimal" : "sin punto decimal",
        //         esFlotante ? "FLOTANTE" : "ENTERO", clasificacion);

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
            if (c == '=' && siguiente == '=') {
                lexema = "==";
                clasificacion = ClasificacionLexica.OP_IGUAL_IGUAL;
                avanzar();
            } else if (c == '<' && siguiente == '>') {
                lexema = "<>";
                clasificacion = ClasificacionLexica.OP_DISTINTO;
                avanzar();
            } else {
            }
        } else {
        }

        if (lexema.length() == 1) {
        } else {
        }

        agregarToken(new Token(clasificacion, lexema, l, col));
    }

    private void reportarError(char c) {
        agregarError("Caracter no reconocido: '" + c + "'", linea, columna);
        avanzar();
    }

    private void agregarToken(Token t) {
    tokens.add(t);

    if (t.getClasificacion() == ClasificacionLexica.IDENTIFICADOR) {
        if (estaEnTablaSimbolos(t.getNombrePropio())) {
            // ── el identificador ya estaba: NO se inserta, solo se avisa ──
          //  System.out.printf("[TABLA] '%s' ya existe (L%d:C%d) -> no se inserta%n",
                   //           t.getNombrePropio(), t.getLinea(), t.getColumna());
        } else {
            // ── nuevo identificador: se inserta y se re-imprime la tabla completa ──
            tablaSimbolos.add(t);
            System.out.printf("[TABLA] + nuevo identificador '%s' en (L%d:C%d) [total=%d]%n",
                              t.getNombrePropio(), t.getLinea(), t.getColumna(),
                              tablaSimbolos.size());
            imprimirTablaSimbolos("TABLA DE SIMBOLOS ACTUALIZADA");
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
    }

    // sew termino el lexema  ? 
    private void trazarFinLexema() {
        if (posicion < texto.length()) {
        } else {
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
        // System.out.println("_____ TOKENS ______");
        // for (Token t : tokens) System.out.println("  " + t);

        // System.out.println("\n___ TABLA DE SIMBOLOS _____");
        // if (tablaSimbolos.isEmpty()) System.out.println("  (vacia)");
        // else for (Token s : tablaSimbolos) System.out.printf("  %-15s -> %d (%s)%n", s.getNombrePropio(), s.getClasificacion(), ClasificacionLexica.nombre(s.getClasificacion()));
        // //%-15s imprime el nombre propio y deja 15 espacios en blanco, %d imprime la clasificacion lexica (entero), %s su nombre, %n salto de linea
        System.out.println("\n____ ERRORES ____");
        if (errores.isEmpty()) System.out.println("  (ninguno)");
        else errores.forEach(e -> System.out.println("  " + e));
    }

    /** Imprime el estado actual de la tabla de simbolos, numerado y con la posicion de cada uno. */
private void imprimirTablaSimbolos(String encabezado) {
    System.out.println();
    System.out.println("--- " + encabezado + " ---");
    if (tablaSimbolos.isEmpty()) {
        System.out.println("  (vacia)");
        return;
    }
    System.out.printf("  %-3s %-20s %-15s %-10s%n",
                      "#", "Nombre", "Clasificacion", "Posicion");
    System.out.println("  " + "-".repeat(55));
    for (int i = 0; i < tablaSimbolos.size(); i++) {
        Token s = tablaSimbolos.get(i);
        System.out.printf("  %-3d %-20s %-15s (L%d:C%d)%n",
                          i + 1,
                          s.getNombrePropio(),
                          ClasificacionLexica.nombre(s.getClasificacion()), // o s.getClasificacion() para el int
                          s.getLinea(),
                          s.getColumna());
    }
    System.out.println("  Total: " + tablaSimbolos.size() + " identificador(es)");
    System.out.println();
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


    // ClasificacionLexica.java (0 = fin de archivo, 1..255 = ASCII del caracter unico, 256+ = demas lexemas)
}
