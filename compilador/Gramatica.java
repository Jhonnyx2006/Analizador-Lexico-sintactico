package compilador;

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
 *   5. (NUEVO) ladosIzquierdos[], simbolosDer[][], cantSimbolosDer[]
 *              -> cada produccion ya separada en simbolos (lo usan First, Follow y el LLDriver)
 */
public class Gramatica {

    // La gramatica no cambia, asi que usamos estructuras estaticas
    static final int NUM_PRODUCCIONES = 33;
    static final int MAX_NO_TERMINALES = 33;   // como maximo uno por produccion
    static final int MAX_TERMINALES = 100;
    static final int MAX_SIMBOLOS_POR_LADO = 10;

    static final String FLECHA = "->";
    static final String EPSILON = "\u03B5";          // letra griega epsilon (escapada para no depender de la codificacion del .java)
    static final String SIMBOLO_INICIAL = "inicio";  // NUEVO: simbolo sentencial S (produccion 33: inicio -> programa)

    // Arreglos
    static String[] gramatica = new String[NUM_PRODUCCIONES];
    static String[] ladosDerechos = new String[NUM_PRODUCCIONES];
    static String[] noTerminales = new String[MAX_NO_TERMINALES];
    static String[] terminales = new String[MAX_TERMINALES];

    // NUEVO: lado izquierdo de cada produccion y lado derecho ya separado en simbolos.
    // simbolosDer[p][0..cantSimbolosDer[p]-1] son los simbolos de la produccion p (0 simbolos = produccion vacia, epsilon)
    static String[] ladosIzquierdos = new String[NUM_PRODUCCIONES];
    static String[][] simbolosDer = new String[NUM_PRODUCCIONES][MAX_SIMBOLOS_POR_LADO];
    static int[] cantSimbolosDer = new int[NUM_PRODUCCIONES];

    // NUEVO: los simbolos como numeros enteros, como los usa un analizador sintactico real.
    //   terminal      -> su clasificacion lexica (0 = $, 1..255 = ASCII, 256+ = identificador, numeros, palabras reservadas...)
    //   no terminal   -> BASE_NO_TERMINAL + su indice en noTerminales[]
    static final int BASE_NO_TERMINAL = 1000; //es para dividir los terminales y no terminales en el arreglo 
    static int[] codigoTerminal = new int[MAX_TERMINALES];                         // clasificacion lexica de terminales[i]
    static int[] columnaPorCodigo = new int[ClasificacionLexica.MAX_CODIGO + 1];   // clasificacion lexica -> columna de la matriz (-1 = no existe)
    static int[][] simbolosDerNum = new int[NUM_PRODUCCIONES][MAX_SIMBOLOS_POR_LADO];   // lado derecho de cada produccion en numeros

    // Cuantas posiciones estan realmente ocupadas en cada arreglo
    static int totalNoTerminales = 0;
    static int totalTerminales = 0;

    public static void main(String[] args) {
        String ruta = "gramatica.txt";

        try {
            cargar(ruta);
        } catch (IOException e) {
            System.out.println("Error al leer la gramatica: " + e.getMessage());
            return;
        }

        imprimirResultados();
    }

    /** NUEVO: ejecuta todos los pasos en orden. Las demas clases solo llaman a este metodo. */
    static void cargar(String ruta) throws IOException {
        leerGramatica(ruta);              // abre, lee linea por linea y cierra el archivo
        extraerLadosDerechos();
        extraerNoTerminales();
        extraerTerminales();
        extraerSimbolosPorProduccion();
        numerarSimbolos();                // NUEVO
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

    /** Cambia la flecha unicode por "->" y elimina la numeracion "1.- " si existe. */
    static String limpiarLinea(String linea) {
        linea = linea.replace("\uFEFF", "").replace("\u2192", FLECHA).trim();

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
            ladosIzquierdos[i] = izquierdo;                          // NUEVO

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
                    continue; // epsilon no es un terminal
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

    /** NUEVO: guarda cada lado derecho separado en simbolos. Una produccion epsilon queda con 0 simbolos. */
    static void extraerSimbolosPorProduccion() {
        for (int i = 0; i < NUM_PRODUCCIONES; i++) {
            int n = separarSimbolos(ladosDerechos[i], simbolosDer[i]);
            if (n == 1 && simbolosDer[i][0].equals(EPSILON)) {
                simbolosDer[i][0] = null;
                n = 0;
            }
            cantSimbolosDer[i] = n;
        }
    }

    /**
     * NUEVO: convierte los simbolos (texto) en numeros, una sola vez, al cargar la gramatica.
     * Despues de esto el analizador sintactico ya no compara textos: compara enteros.
     */
    static void numerarSimbolos() {
        for (int c = 0; c < columnaPorCodigo.length; c++) {
            columnaPorCodigo[c] = -1;
        }

        for (int i = 0; i < totalTerminales; i++) {
            codigoTerminal[i] = ClasificacionLexica.codigoDeTerminal(terminales[i]);
            columnaPorCodigo[codigoTerminal[i]] = i;
        }
        columnaPorCodigo[ClasificacionLexica.FIN_ARCHIVO] = totalTerminales;   // la ultima columna es $

        for (int p = 0; p < NUM_PRODUCCIONES; p++) {
            for (int j = 0; j < cantSimbolosDer[p]; j++) {
                simbolosDerNum[p][j] = numeroDe(simbolosDer[p][j]);
            }
        }
    }

    /** Numero de un simbolo escrito como texto: BASE_NO_TERMINAL + indice, o su clasificacion lexica. */
    static int numeroDe(String simbolo) {
        int nt = indiceNoTerminal(simbolo);
        if (nt >= 0) {
            return BASE_NO_TERMINAL + nt;
        }
        return ClasificacionLexica.codigoDeTerminal(simbolo);
    }

    static boolean esNoTerminalNum(int simbolo) {
        return simbolo >= BASE_NO_TERMINAL;
    }

    /** Columna de la matriz que le toca a una clasificacion lexica, o -1 si la gramatica no la usa (ej. '/'). */
    static int columnaDeCodigo(int clasificacion) {
        if (clasificacion < 0 || clasificacion > ClasificacionLexica.MAX_CODIGO) {
            return -1;
        }
        return columnaPorCodigo[clasificacion];
    }

    /** Nombre de un simbolo numerico, para las trazas y los mensajes de error. */
    static String nombreSimbolo(int simbolo) {
        if (esNoTerminalNum(simbolo)) {
            return noTerminales[simbolo - BASE_NO_TERMINAL];
        }
        int columna = columnaDeCodigo(simbolo);
        if (columna == totalTerminales) {
            return "$";
        }
        if (columna >= 0) {
            return terminales[columna];
        }
        if (simbolo > 0 && simbolo < 256) {
            return String.valueOf((char) simbolo);   // caracter unico que la gramatica no usa
        }
        return ClasificacionLexica.nombre(simbolo);
    }

    // ------------------------------------------------------------------
    // NUEVO: consultas que usan First, Follow, la matriz y el LLDriver
    // ------------------------------------------------------------------
    static int indiceNoTerminal(String simbolo) {
        for (int i = 0; i < totalNoTerminales; i++) {
            if (noTerminales[i].equals(simbolo)) {
                return i;
            }
        }
        return -1;
    }

    static int indiceTerminal(String simbolo) {
        for (int i = 0; i < totalTerminales; i++) {
            if (terminales[i].equals(simbolo)) {
                return i;
            }
        }
        return -1;
    }

    static boolean esNoTerminal(String simbolo) {
        return indiceNoTerminal(simbolo) >= 0;
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** Busca un simbolo dentro de las primeras 'cantidad' posiciones del arreglo. */
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
     * Devuelve cuantos simbolos guardo en 'salida'.
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
                // palabra: id, lista_sent, Programa, epsilon, ...
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
                // simbolo de un solo caracter: ( ) ; , = + - * > <
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
        System.out.println("=== 1. Gramatica completa (" + NUM_PRODUCCIONES + " producciones) ===");
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

        System.out.println("\n=== 5. Clasificacion lexica (entero) de cada terminal ===");
        for (int i = 0; i < totalTerminales; i++) {
            System.out.println((i + 1) + ".- " + terminales[i] + " -> " + codigoTerminal[i]);
        }
        System.out.println("$ -> " + ClasificacionLexica.FIN_ARCHIVO);
    }
}
