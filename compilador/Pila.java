package compilador;

import java.util.EmptyStackException;

//tope - 1 es la cima 
//tambien cambiamos de lista enlazada  a arreglo String y entero tope 
public class Pila {

  
    private  String[] datos = new String[2];
    private int tope = 0;                         // cuantos elementos hay guardados

    public void push(String simbolo) {
         if (tope == datos.length) {
            String[] nuevo = new String[datos.length * 2];
            System.arraycopy(datos, 0, nuevo, 0, tope);
            datos = nuevo; //cambiamos la referencia
        }
     
        datos[tope] = simbolo;
        tope++;
    }

    public String pop() {
        if (tope == 0) {
            throw new EmptyStackException();
        }
        tope--;
        return datos[tope];
    }

    public String top() {
        if (tope == 0) {
            throw new EmptyStackException();
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
