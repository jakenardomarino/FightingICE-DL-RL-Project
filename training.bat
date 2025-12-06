@ECHO OFF
java -cp FightingICE.jar;./lib/*;./lib/lwjgl/*;./lib/lwjgl/natives/windows/amd64/*;./lib/grpc/*; Main --limithp 400 400 -t -off

REM This runs the game in training mode and, importantly, doesn't log the output.
REM The -t flag enables training mode, where both P1 and P2 have infinite energy.
REM The -off flag disables logging.