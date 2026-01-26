. Soma os numeros de 1 ate 5
INICIO  LDA     #0      . Limpa Acumulador (Soma)
        LDX     #1      . X sera nosso contador
LOOP    ADD     #1      . Adiciona 1 (Simulando o contador no A)
        TIX     #5      . Incrementa X e compara com 5
        JLT     LOOP    . Se X < 5, volta para o LOOP
        STA     RESULT  . Salva o resultado final (15 decimal / 0F hex)
        RSUB
RESULT  RESW    1