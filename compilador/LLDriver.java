package compilador;

/**
 * Analizador sintactico predictivo (LLDriver).
 *
 *   Push(S)
 *   x = cima de la pila, a = token de entrada
 *   mientras la pila no este vacia:
 *       si x es no terminal:  si Predict[x,a] != 0 -> Pop() y Push() de la produccion al reves
 *                             si no -> error de sintaxis
 *       si x es terminal:     si x == a -> Pop() y a = scanner()
 *                             si no -> error de sintaxis
 */
public class LLDriver {

    private final AnalizadorLexico lexico;
    private final Pila pila = new Pila();

    private AnalizadorLexico.Token token;   // token actual (trae lexema, linea y columna para los errores)
    private String a;                       // simbolo de la gramatica que representa al token actual

    public LLDriver(AnalizadorLexico lexico) {
        this.lexico = lexico;
    }

    /** scanner(): pide UN token al analizador lexico y lo convierte en un terminal de la gramatica. */
    private String scanner() {
        token = lexico.siguienteToken();

        switch (token.getAtributo()) {
            case identificadores: return "id";
            case enteros:         return "literalentera";
            case flotante:        return "literalreal";
            case finDeArchivo:    return FirstFollow.DOLAR;
            default:              return token.getNombrePropio();   // palabras reservadas y caracteres simples
        }
    }

    /** Regresa true si la entrada pertenece al lenguaje. */
    public boolean analizar() {
        pila.push(Gramatica.SIMBOLO_INICIAL);       // Push(S)
        a = scanner();

        while (!pila.estaVacia()) {
            String x = pila.top();
            System.out.printf("%n[SINT] Pila (cima a la izquierda): %s%n", pila);
            System.out.printf("[SINT] x = %s | a = %s (token '%s')%n", x, a, token.getNombrePropio());

            if (Gramatica.esNoTerminal(x)) {
                int prod = MatrizPredictiva.consultar(x, a);

                if (prod != 0) {
                    System.out.printf("[SINT] %s es no terminal: Predict[%s,%s] = %d -> %s%n",
                            x, x, a, prod, Gramatica.gramatica[prod - 1]);

                    pila.pop();                                                    // Pop() de x
                    for (int i = Gramatica.cantSimbolosDer[prod - 1] - 1; i >= 0; i--) {
                        pila.push(Gramatica.simbolosDer[prod - 1][i]);             // Push() del lado derecho, de derecha a izquierda
                    }
                    // AQUI (fase semantica): se aplico la produccion 'prod'
                } else {
                    errorNoTerminal(x);
                    return false;
                }
            } else {
                if (x.equals(a)) {
                    System.out.printf("[SINT] '%s' es terminal y coincide con la entrada: Pop() y pido el siguiente token%n", x);
                    pila.pop();
                    // AQUI (fase semantica): 'token' acaba de ser aceptado (lexema, linea, columna)
                    a = scanner();
                } else {
                    errorTerminal(x);
                    return false;
                }
            }
        }

        // La pila quedo vacia: solo es valido si tambien se acabo la entrada
        if (!a.equals(FirstFollow.DOLAR)) {
            System.out.printf("%n[SINT] ERROR (L%d:C%d): el programa ya termino pero sobra '%s'%n",
                    token.getLinea(), token.getColumna(), token.getNombrePropio());
            return false;
        }

        System.out.println("\n[SINT] Pila vacia y entrada terminada: el programa es sintacticamente correcto.");
        return true;
    }

    private void errorTerminal(String esperado) {
        System.out.printf("%n[SINT] ERROR DE SINTAXIS (L%d:C%d): se esperaba '%s' y se encontro %s%n",
                token.getLinea(), token.getColumna(), esperado, descripcionToken());
    }

    private void errorNoTerminal(String x) {
        System.out.printf("%n[SINT] ERROR DE SINTAXIS (L%d:C%d): se encontro %s y no hay produccion para %s con ese token.%n",
                token.getLinea(), token.getColumna(), descripcionToken(), x);

        // Los terminales que si se aceptarian aqui son las columnas con algo distinto de 0 en la fila de x
        int fila = Gramatica.indiceNoTerminal(x);
        String esperados = "";
        for (int c = 0; c <= Gramatica.totalTerminales; c++) {
            if (MatrizPredictiva.predict[fila][c] != 0) {
                esperados += " " + MatrizPredictiva.nombreColumna(c);
            }
        }
        System.out.println("[SINT] En este punto se esperaba alguno de:" + esperados);
    }

    private String descripcionToken() {
        if (a.equals(FirstFollow.DOLAR)) {
            return "el fin del archivo";
        }
        return "'" + token.getNombrePropio() + "'";
    }
}
