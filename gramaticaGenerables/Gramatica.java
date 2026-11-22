import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 *   1. gramatica[]      -> las 33 producciones completas
 *   2. ladosDerechos[]  -> solo el lado derecho de cada produccion
 *   3. noTerminales[]   -> simbolos no terminales, sin repetir
 *   4. terminales[]     -> simbolos terminales, sin repetir
 */
public class Gramatica {

    // La gramitica no cambia asi que usamos esteucutras estaticas 
    static final int NUM_PRODUCCIONES = 33;
    static final int MAX_NO_TERMINALES = 33;   // como miximo uno por produccion
    static final int MAX_TERMINALES = 100;
    static final int MAX_SIMBOLOS_POR_LADO = 10;

    static final String FLECHA = "->";
    static final String EPSILON = "ε";

    // Arreglos 
    static String[] gramatica = new String[NUM_PRODUCCIONES];
    static String[] ladosDerechos = new String[NUM_PRODUCCIONES];
    static String[] noTerminales = new String[MAX_NO_TERMINALES];
    static String[] terminales = new String[MAX_TERMINALES];

    // Cuintas posiciones estin realmente ocupadas en cada arreglo
    static int totalNoTerminales = 0;
    static int totalTerminales = 0;

    public static void main(String[] args) {
        String ruta = (args.length > 0) ? args[0] : "gramatica.txt";

        try {
            //    leer el archivo linea por linea y cerrarlo
            leerGramatica(ruta);
        } catch (IOException e) {
            System.out.println("Error al leer la gramitica: " + e.getMessage());
            return;
        }

        // lados derechos
        extraerLadosDerechos();

        //  no terminales (lados izquierdos sin repetir)
        extraerNoTerminales();

        //  terminales (simbolos de los lados derechos que no son no terminales)
        extraerTerminales();

        imprimirResultados();
    }
 
    
    static void leerGramatica(String ruta) throws IOException {
        int n = 0;
        BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(ruta), StandardCharsets.UTF_8));
        try {
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = limpiarLinea(linea);
                if (linea.length() == 0) {
                    continue; // ignora lineas en blanco
                }
                if (linea.indexOf(FLECHA) < 0) {
                    throw new IOException("Linea sin '" + FLECHA + "': " + linea);
                }
                if (n == NUM_PRODUCCIONES) {
                    throw new IOException("El archivo tiene mis de " + NUM_PRODUCCIONES + " producciones.");
                }
                gramatica[n] = linea;
                n++;
            }
        } finally {
            lector.close(); // el archivo se cierra siempre, aunque haya error
        }

        if (n != NUM_PRODUCCIONES) {
            throw new IOException("Se esperaban " + NUM_PRODUCCIONES
                    + " producciones y se leyeron " + n + ".");
        }
    }

    /** cambia "→" por "->" y elimina la numeracion "1.- " si existe. */
    static String limpiarLinea(String linea) {
        linea = linea.replace("\uFEFF", "").replace("→", FLECHA).trim();

        int p = linea.indexOf(".-");
        if (p > 0) {
            boolean soloDigitos = true;
            for (int i = 0; i < p; i++) {
                if (!Character.isDigit(linea.charAt(i))) {
                    soloDigitos = false;
                }
            }
            if (soloDigitos) {
                linea = linea.substring(p + 2).trim();
            }
        }
        return linea;
    }

    static void extraerLadosDerechos() {
        for (int i = 0; i < NUM_PRODUCCIONES; i++) {
            int p = gramatica[i].indexOf(FLECHA);
            // se corta a la derecha de "->" sin incluirlo
            ladosDerechos[i] = gramatica[i].substring(p + FLECHA.length()).trim();
        }
    }


    static void extraerNoTerminales() {
        for (int i = 0; i < NUM_PRODUCCIONES; i++) {
            int p = gramatica[i].indexOf(FLECHA);
            String izquierdo = gramatica[i].substring(0, p).trim(); // sin "->"

            if (!estaEn(izquierdo, noTerminales, totalNoTerminales)) {
                noTerminales[totalNoTerminales] = izquierdo;
                totalNoTerminales++;
            }
        }
    }


    static void extraerTerminales() {
        String[] simbolos = new String[MAX_SIMBOLOS_POR_LADO];

        for (int i = 0; i < NUM_PRODUCCIONES; i++) {
            int n = separarSimbolos(ladosDerechos[i], simbolos);

            for (int j = 0; j < n; j++) {
                String s = simbolos[j];

                if (s.equals(EPSILON)) {
                    continue; // ε no es un terminal
                }
                if (estaEn(s, noTerminales, totalNoTerminales)) {
                    continue; // es un no terminal
                }
                if (estaEn(s, terminales, totalTerminales)) {
                    continue; // ya estaba guardado
                }
                terminales[totalTerminales] = s;
                totalTerminales++;
            }
        }
    }


   // /** Busca un simbolo dentro de las primeras 'cantidad' posiciones del arreglo. */
    static boolean estaEn(String simbolo, String[] arreglo, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            if (arreglo[i].equals(simbolo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Separa un lado derecho en simbolos. Funciona con o sin espacios
     * alrededor de la puntuacion: "( lista_id ) ;" y "(lista_id);" dan lo mismo.
     * Devuelve cuintos simbolos guardo en 'salida'.
     */
    static int separarSimbolos(String texto, String[] salida) {
        int n = 0;
        int i = 0;
        int largo = texto.length();

        while (i < largo) {
            char c = texto.charAt(i);

            if (c == ' ' || c == '\t') {
                i++;
            } else if (Character.isLetterOrDigit(c) || c == '_') {
                // palabra: id, lista_sent, Programa, ε, ...
                int inicio = i;
                while (i < largo && (Character.isLetterOrDigit(texto.charAt(i)) || texto.charAt(i) == '_')) {
                    i++;
                }
                salida[n] = texto.substring(inicio, i);
                n++;
            } else if (i + 1 < largo
                    && ((c == '<' && texto.charAt(i + 1) == '>') || (c == '=' && texto.charAt(i + 1) == '='))) {
                // operadores de dos caracteres: <> y ==
                salida[n] = texto.substring(i, i + 2);
                n++;
                i += 2;
            } else {
                // simbolo de un solo caricter: ( ) ; , = + - * > <
                salida[n] = String.valueOf(c);
                n++;
                i++;
            }

            if (n == salida.length) {
                throw new IllegalStateException("Lado derecho con demasiados simbolos: " + texto);
            }
        }
        return n;
    }

   
    static void imprimirResultados() {
        System.out.println("=== 1. Gramitica completa (" + NUM_PRODUCCIONES + " producciones) ===");
        for (int i = 0; i < NUM_PRODUCCIONES; i++) {
            System.out.println((i + 1) + ".- " + gramatica[i]);
        }

        System.out.println("\n=== 2. Lados derechos ===");
        for (int i = 0; i < NUM_PRODUCCIONES; i++) {
            System.out.println((i + 1) + ".- " + ladosDerechos[i]);
        }

        System.out.println("\n=== 3. No terminales (" + totalNoTerminales + ") ===");
        for (int i = 0; i < totalNoTerminales; i++) {
            System.out.println((i + 1) + ".- " + noTerminales[i]);
        }

        System.out.println("\n=== 4. Terminales (" + totalTerminales + ") ===");
        for (int i = 0; i < totalTerminales; i++) {
            System.out.println((i + 1) + ".- " + terminales[i]);
        }
    }
}
