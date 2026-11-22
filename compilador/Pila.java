package compilador;

/**
 * Pila de simbolos lo mas simple posible: un arreglo de tamano fijo y un contador.
 * datos[tope - 1] es la cima.
 */
public class Pila {

    private static final int CAPACIDAD = 1000;   // sobra: la pila solo crece con el anidamiento del programa

    private final String[] datos = new String[CAPACIDAD];
    private int tope = 0;                         // cuantos elementos hay guardados

    public void push(String simbolo) {
        if (tope == CAPACIDAD) {
            throw new IllegalStateException("Pila llena");
        }
        datos[tope] = simbolo;
        tope++;
    }

    public String pop() {
        if (tope == 0) {
            throw new IllegalStateException("Pila vacia");
        }
        tope--;
        return datos[tope];
    }

    public String top() {
        if (tope == 0) {
            throw new IllegalStateException("Pila vacia");
        }
        return datos[tope - 1];
    }

    public boolean estaVacia() {
        return tope == 0;
    }

    /** Se muestra con la cima a la izquierda: "lista_sent Fin" significa que lista_sent es la cima. */
    @Override
    public String toString() {
        String texto = "[ ";
        for (int i = tope - 1; i >= 0; i--) {
            texto += datos[i] + " ";
        }
        return texto + "]";
    }
}
