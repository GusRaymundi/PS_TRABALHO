. Soma os numeros de 6 a 10
INICIO2  LDA     #0      ; limpa acumulador
CONT2     LDX    #6      ; contador inicial
LOOP2     ADD    #1      ; incrementa
          TIX    #10     ; compara X com 10
          JLT    LOOP2   ; volta se X < 10
          STA    RESULT2 ; salva resultado
          RSUB
RESULT2   RESW    1