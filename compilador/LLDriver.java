package compilador;

/**
 * Analizador sintactico predictivo (LLDriver).
 *
 *   Push(S)
 *   x = cima de la pila, a = clasificacion lexica del token de entrada
 *   mientras la pila no este vacia:
 *       si x es no terminal:  si Predict[x,a] != 0 -> Pop() y Push() de la produccion al reves
 *                             si no -> error de sintaxis
 *       si x es terminal:     si x == a -> Pop() y a = scanner()
 *                             si no -> error de sintaxis
 *
 * x y a son enteros: la clasificacion lexica de un token ES el numero del terminal en la gramatica,
 * por eso "x == a" es una comparacion de enteros y no hace falta traducir nada.
 */
public class LLDriver {

    private final AnalizadorLexico lexico;
    private final Pila pila = new Pila();

    private AnalizadorLexico.Token token;   // token actual (trae nombre propio, linea y columna para los errores)
    private int a;                          // clasificacion lexica del token actual

    public LLDriver(AnalizadorLexico lexico) {
        this.lexico = lexico; //injeccion de dependencias 
    }

    /** scanner(): pide UN token al analizador lexico y regresa su clasificacion lexica (entero). */
    //si nos mandan un nombre de Id no podemos poner su nobre propcio 
    private int scanner() {
        token = lexico.siguienteToken();
        return token.getClasificacion();
    }

    /** Regresa true si la entrada pertenece al lenguaje. */
    public boolean analizar() {
        pila.push(Gramatica.BASE_NO_TERMINAL + Gramatica.indiceNoTerminal(Gramatica.SIMBOLO_INICIAL));   // Push(S)
        a = scanner();

        while (!pila.estaVacia()) {
            int x = pila.top();
            System.out.printf("%n[SINT] Pila (cima a la izquierda): %s%n", pila);
            System.out.printf("[SINT] x = %s (%d) | a = %d, nombre propio '%s'%n",
                    Gramatica.nombreSimbolo(x), x, a, token.getNombrePropio());

            if (Gramatica.esNoTerminalNum(x)) {
                int prod = MatrizPredictiva.consultar(x, a);

                if (prod != 0) {
                    System.out.printf("[SINT] %s es no terminal: Predict[%s,%d] = %d -> %s%n",
                            Gramatica.nombreSimbolo(x), Gramatica.nombreSimbolo(x), a, prod, Gramatica.gramatica[prod - 1]);

                    pila.pop();                                                    // Pop() de x
                    for (int i = Gramatica.cantSimbolosDer[prod - 1] - 1; i >= 0; i--) {
                        pila.push(Gramatica.simbolosDerNum[prod - 1][i]);          // Push() del lado derecho, de derecha a izquierda
                    } ///Replace x With Production[ Predict[x,a] ] - Un Pop() y un ciclo de Push()
                    //hacemos un push inverso para leer el mas a la izquierda
                    // AQUI (fase semantica): se aplico la produccion 'prod'
                } else {
                    errorNoTerminal(x);
                    return false;
                }
            } else {
                if (x == a) {
                    System.out.printf("[SINT] %d es terminal y coincide con la entrada ('%s'): Pop() y pido el siguiente token%n",
                            x, token.getNombrePropio());
                    pila.pop();
                    // AQUI (fase semantica): 'token' acaba de ser aceptado (nombre propio, clasificacion, linea, columna)
                    a = scanner();
                } else {
                    errorTerminal(x);
                    return false;
                }
            }
        }

        // La pila quedo vacia: solo es valido si tambien se acabo la entrada
        if (a != ClasificacionLexica.FIN_ARCHIVO) {
            System.out.printf("%n[SINT] ERROR (L%d:C%d): el programa ya termino pero sobra '%s'%n",
                    token.getLinea(), token.getColumna(), token.getNombrePropio());
            return false;
        }

        System.out.println("\n[SINT] Pila vacia y entrada terminada: el programa es sintacticamente correcto.");
        return true;
    }

    private void errorTerminal(int esperado) {
        System.out.printf("%n[SINT] ERROR DE SINTAXIS (L%d:C%d): se esperaba '%s' (%d) y se encontro %s (%d)%n",
                token.getLinea(), token.getColumna(), Gramatica.nombreSimbolo(esperado), esperado, descripcionToken(), a);
    }

    private void errorNoTerminal(int x) {
        System.out.printf("%n[SINT] ERROR DE SINTAXIS (L%d:C%d): se encontro %s (%d) y no hay produccion para %s con ese token.%n",
                token.getLinea(), token.getColumna(), descripcionToken(), a, Gramatica.nombreSimbolo(x));

        // Los terminales que si se aceptarian aqui son las columnas con algo distinto de 0 en la fila de x
        int fila = x - Gramatica.BASE_NO_TERMINAL;
        String esperados = "";
        for (int c = 0; c <= Gramatica.totalTerminales; c++) {
            if (MatrizPredictiva.predict[fila][c] != 0) {
                esperados += " " + MatrizPredictiva.nombreColumna(c);
            }
        }
        System.out.println("[SINT] En este punto se esperaba alguno de:" + esperados);
    }

    private String descripcionToken() {
        if (a == ClasificacionLexica.FIN_ARCHIVO) {
            return "el fin del archivo";
        }
        return "'" + token.getNombrePropio() + "'";
    }
}
