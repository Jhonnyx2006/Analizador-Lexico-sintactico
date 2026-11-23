package compilador;

/**
 * Clasificacion lexica de cada lexema, como numero entero (el "y.tab.h" de los compiladores reales).
 *
 *     0          -> fin de la entrada ($)
 *     1 .. 255   -> caracter unico: su propio codigo ASCII   (';' = 59, '=' = 61, '+' = 43, ...)
 *     256 en adelante -> lexemas de mas de un caracter: identificadores, numeros, "==", "<>" y palabras reservadas
 *
 * El analizador lexico entrega estos numeros y el analizador sintactico los usa directamente
 * como simbolos terminales: ya no hay nombres en texto de por medio.
 */
public final class ClasificacionLexica {

    private ClasificacionLexica() { }

    public static final int FIN_ARCHIVO = 0;

    public static final int IDENTIFICADOR  = 256;
    public static final int LITERAL_ENTERA = 257;
    public static final int LITERAL_REAL   = 258;
    public static final int OP_IGUAL_IGUAL = 259;   // ==
    public static final int OP_DISTINTO    = 260;   // <>

    public static final int PR_PROGRAMA = 261;
    public static final int PR_REAL     = 262;
    public static final int PR_ENTERO   = 263;
    public static final int PR_LEER     = 264;
    public static final int PR_ESCRIBIR = 265;
    public static final int PR_ENTONCES = 266;
    public static final int PR_SI       = 267;
    public static final int PR_SINO     = 268;
    public static final int PR_INICIO   = 269;
    public static final int PR_FIN      = 270;

    public static final int MAX_CODIGO = 270;
    public static final int NO_RESERVADA = -1;

    // Tabla de palabras reservadas: texto y clasificacion en arreglos paralelos
    private static final String[] PALABRAS_RESERVADAS = {
        "Programa", "Real", "Entero", "Leer", "Escribir", "Entonces", "Si", "Sino", "Inicio", "Fin"
    };
    private static final int[] CODIGOS_PALABRAS = {
        PR_PROGRAMA, PR_REAL, PR_ENTERO, PR_LEER, PR_ESCRIBIR, PR_ENTONCES, PR_SI, PR_SINO, PR_INICIO, PR_FIN
    };

    // Nombres para las trazas; el de la clasificacion c esta en NOMBRES[c - 256]
    private static final String[] NOMBRES = {
        "IDENTIFICADOR", "LITERAL_ENTERA", "LITERAL_REAL", "OP_IGUAL_IGUAL", "OP_DISTINTO",
        "PR_PROGRAMA", "PR_REAL", "PR_ENTERO", "PR_LEER", "PR_ESCRIBIR",
        "PR_ENTONCES", "PR_SI", "PR_SINO", "PR_INICIO", "PR_FIN"
    };

    /** Clasificacion de una palabra reservada, o NO_RESERVADA (-1) si el lexema no esta en la tabla. */
    public static int codigoPalabraReservada(String lexema) {
        for (int i = 0; i < PALABRAS_RESERVADAS.length; i++) {
            if (PALABRAS_RESERVADAS[i].equals(lexema)) {
                return CODIGOS_PALABRAS[i];
            }
        }
        return NO_RESERVADA;
    }

    /** Nombre corto de una clasificacion, solo para imprimir. */
    public static String nombre(int codigo) {
        if (codigo == FIN_ARCHIVO) {
            return "FIN_ARCHIVO";
        }
        if (codigo < 256) {
            return "ASCII";                      // caracter unico
        }
        if (codigo <= MAX_CODIGO) {
            return NOMBRES[codigo - 256];
        }
        return "DESCONOCIDO";
    }

    /**
     * Clasificacion que le corresponde a un terminal de la gramatica escrito como texto
     * ("id", "Programa", ";", "<>", ...). Se usa una sola vez, al numerar los simbolos de la gramatica.
     */
    public static int codigoDeTerminal(String simbolo) {
        if (simbolo.equals("$"))             return FIN_ARCHIVO;
        if (simbolo.equals("id"))            return IDENTIFICADOR;
        if (simbolo.equals("literalentera")) return LITERAL_ENTERA;
        if (simbolo.equals("literalreal"))   return LITERAL_REAL;
        if (simbolo.equals("=="))            return OP_IGUAL_IGUAL;
        if (simbolo.equals("<>"))            return OP_DISTINTO;

        int palabra = codigoPalabraReservada(simbolo);
        if (palabra != NO_RESERVADA) {
            return palabra;
        }
        if (simbolo.length() == 1) {
            return simbolo.charAt(0);            // caracter unico: su codigo ASCII
        }
        throw new IllegalArgumentException("Terminal sin clasificacion lexica: " + simbolo);
    }
}
