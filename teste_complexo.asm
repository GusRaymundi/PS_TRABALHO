. ================================================
. PROGRAMA: Fatorial + Soma de Vetor + Aritmetica
. Testa: JSUB/RSUB, loops, formato 2, indexado
. ================================================
. Resultado esperado ao final:
.   RESFAT = 000078 (120 decimal = 5!)
.   SOMA   = 000064 (100 decimal = 10+20+30+40)
.   DOBRO  = 0000F0 (240 decimal = 120*2)
.   FINAL  = 00008C (140 decimal = 240-100)
. ================================================
TESTE    START   0
. --- Salva retorno e inicializa ---
         STL    RETMAIN      ; salva L=0 (retorno do sistema)
         LDA    NUM          ; A = 5
         STA    NFAT         ; NFAT = 5
. --- Chama subrotina de fatorial ---
         JSUB   FATORIAL     ; retorna com A = 5! = 120
         STA    RESFAT       ; RESFAT = 120
. --- Soma os elementos do vetor (indexado) ---
         CLEAR  X            ; X = 0 (indice)
         CLEAR  A            ; A = 0 (acumulador)
         LDS    #12          ; S = 12 (limite: 4 words * 3 bytes)
         LDT    #3           ; T = 3  (incremento por word)
LOOPSOMA ADD    VETOR,X      ; A += VETOR[X]
         ADDR   T,X          ; X += 3
         COMPR  X,S          ; compara X com 12
         JLT    LOOPSOMA     ; se X < 12, volta
. --- A = 100, salva ---
         STA    SOMA         ; SOMA = 100
. --- Dobra o fatorial com ADDR ---
         LDA    RESFAT       ; A = 120
         RMO    A,S          ; S = 120
         ADDR   S,A          ; A = 120 + 120 = 240
         STA    DOBRO        ; DOBRO = 240
. --- Subtrai soma do dobro ---
         LDA    DOBRO        ; A = 240
         SUB    SOMA         ; A = 240 - 100 = 140
         STA    FINAL        ; FINAL = 140
. --- Fim do programa principal ---
         LDL    RETMAIN      ; restaura L=0
         RSUB                ; retorna para endereco 0 (fim)
. ================================================
. SUBROTINA: Fatorial iterativo de NFAT
. Entrada: NFAT (word na memoria, valor N)
. Saida:   A = N!
. Algoritmo: resultado=1, enquanto NFAT>1: resultado*=NFAT, NFAT--
. ================================================
FATORIAL STL    RETFAT       ; salva endereco de retorno
         LDA    #1           ; resultado = 1
LOOPFAT  MUL    NFAT         ; resultado *= NFAT
         STA    TMPFAT       ; salva resultado parcial
         LDA    NFAT         ; carrega contador
         SUB    #1           ; contador--
         STA    NFAT         ; salva contador
         COMP   #1           ; contador == 1?
         LDA    TMPFAT       ; recarrega resultado parcial
         JGT    LOOPFAT      ; se contador > 1, continua
. --- contador chegou em 1, resultado esta em A ---
         LDL    RETFAT       ; restaura endereco de retorno
         RSUB                ; volta pro chamador
. ================================================
. AREA DE DADOS
. ================================================
NUM      WORD   5            ; N = 5
NFAT     WORD   0            ; contador do fatorial
TMPFAT   WORD   0            ; resultado parcial do fatorial
RESFAT   WORD   0            ; resultado final do fatorial
SOMA     WORD   0            ; soma do vetor
DOBRO    WORD   0            ; fatorial * 2
FINAL    WORD   0            ; dobro - soma
RETMAIN  WORD   0            ; endereco de retorno do main
RETFAT   WORD   0            ; endereco de retorno da subrotina
VETOR    WORD   10           ; VETOR[0] = 10
         WORD   20           ; VETOR[1] = 20
         WORD   30           ; VETOR[2] = 30
         WORD   40           ; VETOR[3] = 40
         END    TESTE