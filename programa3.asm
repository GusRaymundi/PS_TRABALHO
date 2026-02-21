PROG3    START   0
         EXTREF  RESULT1   . Avisa que RESULT1 está em outro módulo
         +LDA    RESULT1   . Carrega o valor que o Prog1 calculou
         ADD     #10       . Soma 10 ao resultado do Prog1
         STA     FINAL     . Salva no seu próprio espaço
         RSUB
FINAL    RESW    1
         END