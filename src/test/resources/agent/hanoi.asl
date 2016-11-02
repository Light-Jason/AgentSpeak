!main.


+!main
    <-
        !!slice/take(0)
.


+!slice/take(T)
    <-
        generic/print("agent", MyID, "tries to take from tower", T);
        S = tower/pop(T);
        generic/print("agent", MyID, "gets", S, "from tower", T);
        !slice/push(MaxTowerNumber, S)
.

-!slice/take(X)
    <-
        generic/print("agent", MyID, "cannot take slice from tower", X);
        T++;
        !slice/take(T)
.



+!push/slice(T, S)
    <-
        generic/print("agent", MyID, "tries to push on tower", T, S);
        tower/push(T,S);
        generic/print("agent", MyID, "pushs on tower", T, S, "success")
.


-!push/slice(T, S)
    : T > 1 <-
        generic/print("agent", MyID, "pushing on tower", T, "with", S, "fails");
        T--;
        !push/slice(T, S)

    : T <= 0 <-
        !push/slice(MaxTowerNumber, S)
.


