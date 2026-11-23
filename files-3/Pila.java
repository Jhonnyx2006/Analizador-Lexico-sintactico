package compilador;

/**
 * Pila de simbolos lo mas simple posible: un arreglo de enteros de tamano fijo y un contador.
 * datos[tope - 1] es la cima.
 *
 * Cada simbolo es un entero:
 *   terminal    -> su clasificacion lexica (la misma que entrega el analizador lexico)
 *   no terminal -> Gramatica.BASE_NO_TERMINAL + su indice
 */
public class Pila {

    private static final int CAPACIDAD = 1000;   // sobra: la pila solo crece con el anidamiento del programa

    private final int[] datos = new int[CAPACIDAD];
    private int tope = 0;                         // cuantos elementos hay guardados

    public void push(int simbolo) {
        if (tope == CAPACIDAD) {
            throw new IllegalStateException("Pila llena");
        }
        datos[tope] = simbolo;
        tope++;
    }

    public int pop() {
        if (tope == 0) {
            throw new IllegalStateException("Pila vacia");
        }
        tope--;
        return datos[tope];
    }

    public int top() {
        if (tope == 0) {
            throw new IllegalStateException("Pila vacia");
        }
        return datos[tope - 1];
    }

    public boolean estaVacia() {
        return tope == 0;
    }

    /** Se muestra con la cima a la izquierda y con los nombres de los simbolos (solo para la traza). */
    @Override
    public String toString() {
        String texto = "[ ";
        for (int i = tope - 1; i >= 0; i--) {
            texto += Gramatica.nombreSimbolo(datos[i]) + " ";
        }
        return texto + "]";
    }
}
