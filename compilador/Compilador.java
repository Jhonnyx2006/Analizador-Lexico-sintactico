package compilador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Punto de entrada: conecta todas las piezas.
 *
 *   gramatica.txt -> Gramatica -> FirstFollow -> MatrizPredictiva ----\
 *                                                                      v
 *   programaEjemplo.txt -> AnalizadorLexico <--- scanner() --- LLDriver (+ Pila)
 */
public class Compilador {

    public static void main(String[] args) {
        String rutaGramatica = "gramatica.txt";
        String rutaPrograma = "programaEjemplo.txt";

        // ---- FASE 0: tablas del analizador sintactico (se construyen una sola vez) ----
        try {
            Gramatica.cargar(rutaGramatica);
            Gramatica.imprimirResultados();
        } catch (IOException e) {
            System.err.println("No se pudo leer la gramatica '" + rutaGramatica + "': " + e.getMessage());
            return;
        }
        FirstFollow.calcular();
        FirstFollow.imprimir();

        MatrizPredictiva.construir();
        MatrizPredictiva.imprimir();

        // ---- Codigo fuente que se va a analizar ----
        String codigo;
        try {
            codigo = Files.readString(Paths.get(rutaPrograma));
        } catch (IOException e) {
            System.err.println("No se pudo leer el archivo '" + rutaPrograma + "': " + e.getMessage());
            return;
        }
        System.out.printf("%nArchivo '%s' leido: %d caracteres.%n", rutaPrograma, codigo.length());

        // ---- FASE 1 y 2: el parser dirige y el scanner entrega un token cada vez que se lo piden ----
        AnalizadorLexico lexico = new AnalizadorLexico(codigo);
        LLDriver parser = new LLDriver(lexico);

        // System.out.println("\n_____ TRAZA DE EJECUCION (lexico + sintactico) _____");
        boolean correcto = parser.analizar();
        // System.out.println("\n_____ FIN DE LA TRAZA _____\n");

          ///lexico.imprimir();   // tokens entregados, tabla de simbolos y errores lexicos

        System.out.println("\nResultado: " + (correcto ? "ANALISIS CORRECTO" : "SE ENCONTRARON ERRORES"));
    }
}
