# Obwody Współbieżne

Projekt implementuje współbieżny solver do wyliczania wartości obwodów boolowskich. Solver pozwala na jednoczesne obliczanie wielu obwodów oraz równoległe przetwarzanie poszczególnych wyrażeń w ramach pojedynczego obwodu.

---

## Obwody Boolowskie

### Definicja

Obwody boolowskie reprezentują wyrażenia logiczne w postaci drzew. Na przykład:

- Wyrażenie `x ∧ (x ∨ ¬y) ∧ (z ∨ y)` można przedstawić jako drzewo operatorów logicznych.
- Obsługiwane są również operatory wieloargumentowe.

Obsługiwane operacje:

- **Stałe:** `true`, `false`
- **Negacja:** `NOT(a)`
- **Koniunkcja:** `AND(a1, a2, ...)` (co najmniej dwa argumenty)
- **Alternatywa:** `OR(a1, a2, ...)` (co najmniej dwa argumenty)
- **Instrukcja warunkowa:** `IF(a, b, c)` - zwraca `b` jeśli `a` jest `true`, w przeciwnym razie `c`
- **Operatory progowe:** `GTx(a1, a2, ..., an)` oraz `LTx(a1, a2, ..., an)`  
  - `GTx`: `true` jeśli co najmniej `x+1` argumentów jest `true`.
  - `LTx`: `true` jeśli co najwyżej `x-1` argumentów jest `true`.

---

## Specyfikacja

### Klasa `Circuit`

Reprezentuje obwód boolowski. Zawiera korzeń drzewa (`CircuitNode`), który opisuje strukturę obwodu.

```java
public class Circuit {
    private final CircuitNode root;

    public final CircuitNode getRoot();
}
