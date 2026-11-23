package compilador;

/**
 * Matriz predictiva (Predict) como arreglo 2D estatico.
 *
 *   predict[fila][columna] = numero de produccion (1..33) o 0 si es error
 *   fila    = indice del no terminal en Gramatica.noTerminales
 *   columna = indice del terminal en Gramatica.terminales; la ultima columna es '$'
 *
 * Regla para cada produccion A -> alfa:
 *   - por cada terminal a en First(alfa): predict[A][a] = numero de la produccion
 *   - si lambda esta en First(alfa): por cada b en Follow(A) (incluido $): predict[A][b] = numero de la produccion
 */
public class MatrizPredictiva {

    static int[][] predict = new int[Gramatica.MAX_NO_TERMINALES][Gramatica.MAX_TERMINALES + 1];
    static int conflictos = 0;

    private static String[] temp = new String[FirstFollow.MAX_ELEMENTOS];

    static void construir() {
        for (int p = 0; p < Gramatica.NUM_PRODUCCIONES; p++) {
            int fila = Gramatica.indiceNoTerminal(Gramatica.ladosIzquierdos[p]);

            int n = FirstFollow.firstDeCadena(Gramatica.simbolosDer[p], 0, Gramatica.cantSimbolosDer[p], temp);

            for (int k = 0; k < n; k++) {
                if (temp[k].equals(FirstFollow.LAMBDA)) {
                    // alfa puede desaparecer: se usa cuando lo que viene es algo del Follow(A)
                    for (int j = 0; j < FirstFollow.cantFollow[fila]; j++) {
                        asignar(fila, columnaDe(FirstFollow.follow[fila][j]), p + 1);
                    }
                } else {
                    asignar(fila, columnaDe(temp[k]), p + 1);
                }
            }
        }
    }

    private static void asignar(int fila, int columna, int produccion) {
        if (predict[fila][columna] != 0 && predict[fila][columna] != produccion) {
            conflictos++;
            System.out.println("CONFLICTO en Predict[" + Gramatica.noTerminales[fila] + "][" + nombreColumna(columna)
                    + "]: producciones " + predict[fila][columna] + " y " + produccion + " (la gramatica no es LL(1))");
            return;
        }
        predict[fila][columna] = produccion;
    }

    /**
     * Predict[x, a]: numero de produccion o 0.
     *   x = numero del no terminal (Gramatica.BASE_NO_TERMINAL + indice)
     *   a = clasificacion lexica del token (la que entrega el analizador lexico)
     * Regresa 0 si el token no pertenece a la gramatica (ej. '/').
     */
    static int consultar(int noTerminal, int clasificacionLexica) {
        int fila = noTerminal - Gramatica.BASE_NO_TERMINAL;
        int columna = Gramatica.columnaDeCodigo(clasificacionLexica);
        if (fila < 0 || fila >= Gramatica.totalNoTerminales || columna < 0) {
            return 0;
        }
        return predict[fila][columna];
    }

    static int columnaDe(String terminal) {
        if (terminal.equals(FirstFollow.DOLAR)) {
            return Gramatica.totalTerminales;       // la ultima columna es $
        }
        return Gramatica.indiceTerminal(terminal);  // -1 si no es un terminal de la gramatica
    }

    static String nombreColumna(int columna) {
        if (columna == Gramatica.totalTerminales) {
            return FirstFollow.DOLAR;
        }
        return Gramatica.terminales[columna];
    }

    static void imprimir() {
        int filas = Gramatica.totalNoTerminales;
        int columnas = Gramatica.totalTerminales + 1;

        System.out.println("\n=== MATRIZ PREDICTIVA ('.' = error) ===");
        System.out.print("Columnas: ");
        for (int c = 0; c < columnas; c++) {
            int codigo = (c == Gramatica.totalTerminales) ? ClasificacionLexica.FIN_ARCHIVO : Gramatica.codigoTerminal[c];
            System.out.print((c + 1) + "=" + nombreColumna(c) + "(" + codigo + ")  ");
        }
        System.out.println();

        System.out.printf("%-18s", "");
        for (int c = 0; c < columnas; c++) {
            System.out.printf("%4d", c + 1);
        }
        System.out.println();

        for (int f = 0; f < filas; f++) {
            System.out.printf("%-18s", Gramatica.noTerminales[f]);
            for (int c = 0; c < columnas; c++) {
                if (predict[f][c] == 0) {
                    System.out.printf("%4s", 0);
                } else {
                    System.out.printf("%4d", predict[f][c]);
                }
            }
            System.out.println();
        }
        System.out.println("Conflictos encontrados: " + conflictos);
    }
}
