package compilador;

/**
 * datos[tope - 1] es la cima.
 *
 * Cada simbolo es un entero:
 *   terminal    -> su clasificacion lexica (la misma que entrega el analizador lexico)
 *   no terminal -> Gramatica.BASE_NO_TERMINAL + su indice
 */
public class Pila {
  // sobra: la pila solo crece con el anidamiento del programa

    private  int[] datos = new int[5];
    private int tope = 0;                         // cuantos elementos hay guardados

    public void push(int simbolo) {
             if (tope == datos.length) {
            int[] nuevo = new int[datos.length * 2];
            System.arraycopy(datos, 0, nuevo, 0, tope);
            datos = nuevo; //cambiamos la referencia
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
