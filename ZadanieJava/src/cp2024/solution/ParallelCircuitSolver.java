package cp2024.solution;

import cp2024.circuit.*;
import cp2024.demo.BrokenCircuitValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;

    // Klasa implementująca równoległe rozwiązywanie obwodów logicznych.
    // Używa puli wątków do równoczesnego przetwarzania węzłów logicznych w obwodzie.
public class ParallelCircuitSolver implements CircuitSolver {
    private final ExecutorService executor = newCachedThreadPool();                     // Pula wątków do równoległego przetwarzania obwodów.
    private final AtomicBoolean stop = new AtomicBoolean(false);                        // Flaga umożliwiająca przerwanie działania solvera.
    
        // Rozwiązuje współbieżnie dany obwód logiczny, zaczynając od jego korzenia.
        // @param: c - obwód logiczny do rozwiązania.
        // @return: Wynik obliczeń w formie CircuitValue.
    @Override
    public CircuitValue solve(Circuit c) {
        if (stop.get())
            return new BrokenCircuitValue();

        Future<Boolean> resultFuture = executor.submit(() -> {
            try {
                return recursiveSolve(c.getRoot());                                     // Rozwiązanie obwodu rozpoczynając od korzenia.
            } catch (InterruptedException e) {
                return null;
            }
        });

        return new ParallelCircuitValue(resultFuture);
    } 

        // Przerywa działanie solvera i kończy wszystkie aktywne zadania.
    @Override
    public void stop() {
        stop.set(true);                                                                 // Ustawia flagę zatrzymania.
        executor.shutdownNow();                                                         // Przerywa wszystkie aktywne zadania w puli wątków.
    }

        // Rekurencyjnie rozwiązuje dany węzeł obwodu.
        // @param: n - węzeł obwodu do rozwiązania.
        // @return: Wynik logiczny obliczeń dla węzła.
        // @throws: InterruptedException - przypadku zatrzymania solvera w trakcie działania.
    private boolean recursiveSolve(CircuitNode n) throws InterruptedException {
        if (stop.get())
            throw new InterruptedException("Computation interrupted");                  // Przerywa działanie, jeśli solver został zatrzymany.

        if (n.getType() == NodeType.LEAF)
            return ((LeafNode) n).getValue();                                           // Dla liścia zwraca jego wartość logiczną.

        CircuitNode[] args = n.getArgs();                                               // Pobiera argumenty węzła.
        
        return switch (n.getType()) {                                                   // Rozwiązuje węzeł w zależności od jego typu.
            case IF -> solveIF(args);
            case AND -> solveAND(args);
            case OR -> solveOR(args);
            case GT -> solveGT(args, ((ThresholdNode) n).getThreshold());
            case LT -> solveLT(args, ((ThresholdNode) n).getThreshold());
            case NOT -> solveNOT(args);
            default -> throw new RuntimeException("Illegal type " + n.getType());
        };
    }

        // Rozwiązuje węzeł typu NOT (negacja).
        // @param: args - argumenty węzła.
        // @return: Wynik logiczny negacji.
        // @throws: InterruptedException - w przypadku przerwania.
    private boolean solveNOT(CircuitNode[] args) throws InterruptedException {
        return !recursiveSolve(args[0]);                                                // Neguje wynik dla jedynego argumentu.
    }

        // Rozwiązuje współbieżnie węzeł typu LT (mniej niż próg).
        // @param: args - argumenty węzła oraz threshold - Próg.
        // @return: Wynik logiczny porównania.
        // @throws: InterruptedException - w przypadku przerwania.
    private boolean solveLT(CircuitNode[] args, int threshold) throws InterruptedException {
        int size = args.length;
        int gotTrue = 0;                                                                // Licznik [a_i] = true.

        if (threshold == 0) return false;                                               // Przypadki brzegowe.
        if (threshold > size) return true;

        ExecutorCompletionService<Boolean> service;
        service = new ExecutorCompletionService<>(executor);                            // Zarządzanie asynchronicznymi zadaniami.
        List<Future<Boolean>> futures = new ArrayList<>(size);                          // Lista wyników zadań. 

        for (CircuitNode c : args)
            futures.add(service.submit(() -> recursiveSolve(c)));                       // Uruchamianie wątków równolegle.

        try {
            for (int i = 0; i < size; i++) {
                if (service.take().get()) 
                    gotTrue++;                                                          
                if (gotTrue >= threshold) {                                             // Jeśli przekroczymy próg zwróć FALSE.
                    for (Future<Boolean> future : futures)                              
                        future.cancel(true);
                    return false;
                }
                if (i - gotTrue > size - threshold) {                                   // Jeśli nie można przekroczyć progu zwróć TRUE.
                    for (Future<Boolean> future : futures) 
                        future.cancel(true);
                    return true;
                }
            } 
        } catch (Exception e) {
            for (Future<Boolean> future : futures)
                future.cancel(true);  
            throw new InterruptedException("Interrupted while solving LT node");
        }

        return true;  
    }

        // Rozwiązuje współbieżnie węzeł typu GT (więcej niż próg).
        // @param: args - argumenty węzła oraz threshold - Próg.
        // @return: Wynik logiczny porównania.
        // @throws: InterruptedException - w przypadku przerwania.
    private boolean solveGT(CircuitNode[] args, int threshold) throws InterruptedException {
        int size = args.length;
        int gotTrue = 0;                                                                // Licznik [a_i] = true.

        if (threshold >= size) return false;                                            // Przypadki brzegowe.

        ExecutorCompletionService<Boolean> service; 
        service = new ExecutorCompletionService<>(executor);                            // Zarządzanie asynchronicznymi zadaniami.
        List<Future<Boolean>> futures = new ArrayList<>(size);                          // Lista wyników zadań. 

        for (CircuitNode c : args)                                                      // Uruchamianie wątków równolegle.
            futures.add(service.submit(() -> recursiveSolve(c)));

        try {
            for (int i = 0; i < size; i++) {
                if (service.take().get()) 
                    gotTrue++;
                if (gotTrue > threshold) {                                              // Jeśli przekroczymy próg zwróć TRUE.
                    for (Future<Boolean> future : futures) 
                        future.cancel(true);
                    return true;
                }
                if (i - gotTrue >= size - threshold) {                                  // Jeśli nie można przekroczyć progu zwróć FALSE.
                    for (Future<Boolean> future : futures) 
                        future.cancel(true);
                    return false;
                }
            } 
        } catch (Exception e) {
            for (Future<Boolean> future : futures)
                future.cancel(true);  
            throw new InterruptedException("Interrupted while solving GT node");
        }

        return false;       
    }
    
        // Rozwiązuje współbieżnie węzeł typu OR (alternatywa).
        // @param: args - argumenty węzła.
        // @return: Wynik logiczny alternatywy.
        // @throws: InterruptedException - w przypadku przerwania.
    private boolean solveOR(CircuitNode[] args) throws InterruptedException {
        int size = args.length;

        ExecutorCompletionService<Boolean> service;
        service = new ExecutorCompletionService<>(executor);                            // Zarządzanie asynchronicznymi zadaniami.
        List<Future<Boolean>> futures = new ArrayList<>(size);                          // Lista wyników zadań. 

        for (CircuitNode c : args)                                                      // Uruchamianie wątków równolegle.
            futures.add(service.submit(() -> recursiveSolve(c)));

        try {
            for (int i = 0; i < size; i++) {
                if (service.take().get()) {                                             // Jeśli któryś argument jest TRUE to zwraca TRUE.
                    for (Future<Boolean> future : futures) 
                        future.cancel(true);
                    return true; 
                }
            } 
        } catch (Exception e) {
            for (Future<Boolean> future : futures)
                future.cancel(true);  
            throw new InterruptedException("Interrupted while solving OR node");
        }  
        
        return false;                                                                   // Jeśli żaden argument nie jest TRUE to zwraca FALSE.
    }

        // Rozwiązuje współbieżnie węzeł typu AND (koniunkcja).
        // @param: args - argumenty węzła.
        // @return: Wynik logiczny koniunkcji.
        // @throws: InterruptedException - w przypadku przerwania.
    private boolean solveAND(CircuitNode[] args) throws InterruptedException {
        int size = args.length;

        ExecutorCompletionService<Boolean> service; 
        service = new ExecutorCompletionService<>(executor);                            // Zarządzanie asynchronicznymi zadaniami.
        List<Future<Boolean>> futures = new ArrayList<>(size);                          // Lista wyników zadań. 

        for (CircuitNode c : args)                                                      // Uruchamianie wątków równolegle.
            futures.add(service.submit(() -> recursiveSolve(c)));

        try {
            for (int i = 0; i < size; i++) {
                if (!service.take().get()) {                                            // Jeśli jakikolwiek argument jest FALSE, przerywa i zwraca FALSE.
                    for (Future<Boolean> future : futures)
                        future.cancel(true);        
                    return false; 
                }
            } 
        } catch (Exception e) {
            for (Future<Boolean> future : futures)
                future.cancel(true);        
            throw new InterruptedException("Interrupted while solving AND node");
        }   
        return true;                                                                    // Jeśli wszystkie argumenty są TRUE, zwraca TRUE.
    }
    
        // Rozwiązuje sekwencyjnie węzeł typu IF (warunek logiczny).
        // @param: args - argumenty węzła.
        // @return: Wynik logiczny węzła IF.
        // @throws: InterruptedException - w przypadku przerwania.
    private boolean solveIF(CircuitNode[] args) throws InterruptedException {
        ExecutorCompletionService<Boolean> service = new ExecutorCompletionService<>(executor);

        // Uruchomienie trzech wątków równoległych
        Future<Boolean> conditionFuture = service.submit(() -> recursiveSolve(args[0]));
        Future<Boolean> trueBranchFuture = service.submit(() -> recursiveSolve(args[1]));
        Future<Boolean> falseBranchFuture = service.submit(() -> recursiveSolve(args[2]));

        try {
            Future<Boolean> firstCompleted = service.take();                            // Pobierz pierwszy zakończony wynik

            if (firstCompleted == conditionFuture) {                                    // Pierwszy skończony wątek to conditionFuture.
                boolean condition = conditionFuture.get();
                if (condition) {                                                        // Przerywamy odpowiednią gałąź i zwracamy wynik.
                    falseBranchFuture.cancel(true);
                    return trueBranchFuture.get();
                } else {
                    trueBranchFuture.cancel(true);
                    return falseBranchFuture.get();
                }
            } else if (firstCompleted == trueBranchFuture) {                            // Pierwszy skończony wątek to trueBranchFuture.
                boolean trueBranchValue = trueBranchFuture.get();
                Future<Boolean> secondCompleted = service.take();                       // Pobierz drugi zakończony wynik

                if (secondCompleted == falseBranchFuture) {                             // Drugi skończony wątek to falseBranchFuture.
                    boolean falseBranchValue = falseBranchFuture.get();
                    if (trueBranchValue == falseBranchValue) {
                        conditionFuture.cancel(true);                                   // Przerwij conditionFuture, bo wynik jest jednoznaczny.
                        return trueBranchValue;                                         // Gałęzie są równe więc zwróć dowolną.
                    } else {
                        boolean condition = conditionFuture.get();                      // Poczekaj na conditionFuture (ostatni wątek).
                        if (condition) {
                            return trueBranchValue;
                        } else {
                            return falseBranchValue;
                        }
                    }
                } else {                                                                // Drugi skończony wątek to conditionFuture.
                    boolean condition = conditionFuture.get();
                    if (condition) {                                                    // Przerywamy odpowiednią gałąź i zwracamy wynik.
                        falseBranchFuture.cancel(true);
                        return trueBranchValue;
                    } else {
                        return falseBranchFuture.get();                                 // Poczekaj na falseBranchFuture (ostatni wątek).
                    }
                }
            } else {                                                                    // Pierwszy skończony wątek to falseBranchFuture.
                boolean falseBranchValue = falseBranchFuture.get();
                Future<Boolean> secondCompleted = service.take();                       // Pobierz drugi zakończony wynik

                if (secondCompleted == trueBranchFuture) {                              // Drugi skończony wątek to trueBranchFuture.
                    boolean trueBranchValue = trueBranchFuture.get();
                    if (trueBranchValue == falseBranchValue) {
                        conditionFuture.cancel(true);                                   // Przerwij conditionFuture, bo wynik jest jednoznaczny.
                        return falseBranchValue;                                        // Gałęzie są równe więc zwróć dowolną.
                    } else {
                        boolean condition = conditionFuture.get();                      // Poczekaj na conditionFuture (ostatni wątek).
                        if (condition) {
                            return trueBranchValue;
                        } else {
                            return falseBranchValue;
                        }
                    }
                } else {                                                                // Drugi skończony wątek to conditionFuture.
                    boolean condition = conditionFuture.get();
                    if (condition) {
                        return trueBranchFuture.get();                                  // Poczekaj na trueBranchFuture (ostatni wątek).
                    } else {
                        trueBranchFuture.cancel(true);
                        return falseBranchValue;
                    }
                }
            }
        } catch (Exception e) {
            conditionFuture.cancel(true);
            trueBranchFuture.cancel(true);
            falseBranchFuture.cancel(true);
            throw new InterruptedException("Interrupted while solving IF node");
        }
    }
}
