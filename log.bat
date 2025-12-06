@ECHO OFF
java -cp FightingICE.jar;./lib/*;./lib/lwjgl/*;./lib/lwjgl/natives/windows/amd64/*;./lib/grpc/*; Main ^
--limithp 400 400 ^
--json ^
-r 100 ^
-n 1 ^
-del ^
--a1 TOVOR_SparseDeep ^
--a2 MctsAi23i ^
--c1 ZEN ^
--c2 ZEN
