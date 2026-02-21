. Soma os numeros de 1 ate 5
PROG1    START   0
         EXTDEF  RESULT1  
INICIO1  LDA     #0      ; limpa acumulador
CONT1     LDX    #1      ; contador
LOOP1     ADD    #1      ; incrementa
          TIX    #5      ; compara X com 5
          JLT    LOOP1   ; volta se X < 5
          STA    RESULT1 ; salva resultado
          RSUB
RESULT1   RESW    1