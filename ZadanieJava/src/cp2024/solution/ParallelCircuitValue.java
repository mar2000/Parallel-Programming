package cp2024.solution;

import cp2024.circuit.CircuitValue;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

public class ParallelCircuitValue implements CircuitValue {
    private final Future<Boolean> value;

    public ParallelCircuitValue(Future<Boolean> value) {
        this.value = value;
    }

    @Override
    public boolean getValue() throws InterruptedException {
        try {
            Boolean result = value.get();
            if (result == null) 
                throw new InterruptedException("Computation interrupted.");
            return result;
        } catch (ExecutionException e) {
            throw new RuntimeException("Error during computation", e);
        }
    }
}


