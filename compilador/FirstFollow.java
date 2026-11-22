package compilador;

/**
 * Conjuntos First y Follow de la gramatica, en arreglos estaticos.
 *
 *   first[i][..]  -> First del no terminal Gramatica.noTerminales[i]  (puede incluir LAMBDA)
 *   follow[i][..] -> Follow del no terminal Gramatica.noTerminales[i] (puede incluir DOLAR)
 *
 * Se calculan aplicando las reglas una y otra vez hasta que ningun conjunto crece (punto fijo).
 */
public class FirstFollow {

    static final String LAMBDA = Gramatica.EPSILON;  // la cadena vacia
    static final String DOLAR = "$";                  // fin de la entrada

    // terminales + lambda + $ es lo maximo que cabe en un conjunto
    static final int MAX_ELEMENTOS = Gramatica.MAX_TERMINALES + 2;

    static String[][] first = new String[Gramatica.MAX_NO_TERMINALES][MAX_ELEMENTOS];
    static int[] cantFirst = new int[Gramatica.MAX_NO_TERMINALES];

    static String[][] follow = new String[Gramatica.MAX_NO_TERMINALES][MAX_ELEMENTOS];
    static int[] cantFollow = new int[Gramatica.MAX_NO_TERMINALES];

    // arreglo de trabajo donde se guarda el First de una cadena de simbolos
    private static String[] temp = new String[MAX_ELEMENTOS];

    static void calcular() {
        calcularFirst();
        calcularFollow();
    }

    // ------------------------------------------------------------------
    // FIRST
    // ------------------------------------------------------------------
    static void calcularFirst() {
        boolean cambio = true;

        while (cambio) {
            cambio = false;

            for (int p = 0; p < Gramatica.NUM_PRODUCCIONES; p++) {
                int x = Gramatica.indiceNoTerminal(Gramatica.ladosIzquierdos[p]);   // X en X -> alfa

                // First(alfa): cubre las reglas 1, 2 y 3 (terminal, lambda y secuencia Y1 Y2 ... Yk)
                int n = firstDeCadena(Gramatica.simbolosDer[p], 0, Gramatica.cantSimbolosDer[p], temp);

                for (int k = 0; k < n; k++) {
                    if (agregar(first, cantFirst, x, temp[k])) {
                        cambio = true;
                    }
                }
            }
        }
    }

    /**
     * First de la cadena cadena[desde .. cant-1], usando los First que ya se tienen.
     *  - si el primer simbolo es terminal, el First es ese terminal
     *  - si es no terminal, se agrega su First (sin lambda); si tiene lambda se sigue con el siguiente simbolo
     *  - si TODOS los simbolos pueden ser lambda (o la cadena esta vacia) se agrega lambda
     * El resultado se guarda en 'salida' y se regresa cuantos elementos tiene.
     */
    static int firstDeCadena(String[] cadena, int desde, int cant, String[] salida) {
        int n = 0;
        boolean todosAnulables = true;

        for (int i = desde; i < cant; i++) {
            String simbolo = cadena[i];
            boolean anulable = false;

            if (Gramatica.esNoTerminal(simbolo)) {
                int f = Gramatica.indiceNoTerminal(simbolo);
                for (int k = 0; k < cantFirst[f]; k++) {
                    String e = first[f][k];
                    if (e.equals(LAMBDA)) {
                        anulable = true;
                    } else if (!Gramatica.estaEn(e, salida, n)) {
                        salida[n] = e;
                        n++;
                    }
                }
            } else {
                if (!Gramatica.estaEn(simbolo, salida, n)) {
                    salida[n] = simbolo;
                    n++;
                }
            }

            if (!anulable) {
                todosAnulables = false;
                break;                  // este simbolo no puede desaparecer: los siguientes ya no cuentan
            }
        }

        if (todosAnulables) {
            salida[n] = LAMBDA;
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------
    // FOLLOW
    // ------------------------------------------------------------------
    static void calcularFollow() {
        // Regla 1: $ en el Follow del simbolo sentencial
        agregar(follow, cantFollow, Gramatica.indiceNoTerminal(Gramatica.SIMBOLO_INICIAL), DOLAR);

        boolean cambio = true;

        while (cambio) {
            cambio = false;

            for (int p = 0; p < Gramatica.NUM_PRODUCCIONES; p++) {
                int a = Gramatica.indiceNoTerminal(Gramatica.ladosIzquierdos[p]);   // A en A -> alfa B beta
                int cant = Gramatica.cantSimbolosDer[p];

                for (int i = 0; i < cant; i++) {
                    String simbolo = Gramatica.simbolosDer[p][i];
                    if (!Gramatica.esNoTerminal(simbolo)) {
                        continue;                                                   // solo los no terminales tienen Follow
                    }
                    int b = Gramatica.indiceNoTerminal(simbolo);                    // B

                    // beta = lo que viene despues de B
                    int n = firstDeCadena(Gramatica.simbolosDer[p], i + 1, cant, temp);
                    boolean betaAnulable = false;

                    for (int k = 0; k < n; k++) {
                        if (temp[k].equals(LAMBDA)) {
                            betaAnulable = true;
                        } else if (agregar(follow, cantFollow, b, temp[k])) {       // Regla 2: First(beta) sin lambda
                            cambio = true;
                        }
                    }

                    // Regla 3: B esta al final, o beta puede ser lambda -> Follow(A) entra al Follow(B)
                    if (betaAnulable) {
                        for (int k = 0; k < cantFollow[a]; k++) {
                            if (agregar(follow, cantFollow, b, follow[a][k])) {
                                cambio = true;
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /** Agrega 'simbolo' al conjunto de la fila indicada si no estaba. Regresa true si el conjunto crecio. */
    static boolean agregar(String[][] conjunto, int[] cantidad, int fila, String simbolo) {
        if (Gramatica.estaEn(simbolo, conjunto[fila], cantidad[fila])) {
            return false;
        }
        conjunto[fila][cantidad[fila]] = simbolo;
        cantidad[fila]++;
        return true;
    }

    static String comoTexto(String[] conjunto, int cantidad) {
        String texto = "{ ";
        for (int i = 0; i < cantidad; i++) {
            if (conjunto[i].equals(LAMBDA)) {
                texto += "lambda";                      // se imprime como palabra para que cualquier consola lo muestre
            } else {
                texto += "'" + conjunto[i] + "'";       // entre comillas para distinguir la coma ',' del separador
            }
            if (i < cantidad - 1) {
                texto += ", ";
            }
        }
        return texto + " }";
    }

    static void imprimir() {
        System.out.println("\n=== CONJUNTOS FIRST ===");
        for (int i = 0; i < Gramatica.totalNoTerminales; i++) {
            System.out.println("First(" + Gramatica.noTerminales[i] + ") = " + comoTexto(first[i], cantFirst[i]));
        }

        System.out.println("\n=== CONJUNTOS FOLLOW ===");
        for (int i = 0; i < Gramatica.totalNoTerminales; i++) {
            System.out.println("Follow(" + Gramatica.noTerminales[i] + ") = " + comoTexto(follow[i], cantFollow[i]));
        }
    }
}
