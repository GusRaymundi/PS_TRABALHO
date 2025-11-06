package executor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Executor {
    static Registrador A = new Registrador("000000");
    static Registrador X = new Registrador("000000");
    static Registrador L = new Registrador("000000");
	static Registrador B = new Registrador("000000");
	static Registrador S = new Registrador("000000");
	static Registrador T = new Registrador("000000");
    static Registrador PC = new Registrador("000000");
    static Registrador SW = new Registrador("000000");
    static Memória pilha = new Memória();
    static int contador2 = 100;

    public static void main(String[] args) {
        try {
            // Cria um objeto File representando o arquivo "input.txt"
            File file = new File("input.txt");
            
            // Cria um FileInputStream para ler bytes do arquivo
            FileInputStream fis = new FileInputStream(file);
            
            // Cria um BufferedReader para ler linhas de texto do FileInputStream
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String linha;
            // Lê cada linha do arquivo até o final
            while ((linha = br.readLine()) != null) {
                // Processa cada linha do arquivo
                String instruçãoHexa = linha.trim(); // Remove espaços em branco
               
                // Decodificação e execução da instrução
                executarInstrução(instruçãoHexa); // Executa a instrução
                System.out.println("Valor de A: " + A.valor);
                System.out.println("Valor de PC: " + PC.valor);
            }
            
            // Fecha o BufferedReader
            br.close();
        } catch (IOException e) {
            // Trata exceções de E/S (leitura do arquivo)
            e.printStackTrace();
        }
        pilha.imprimir();
    }

    // Método para decodificar e executar a instrução
    public static void executarInstrução(String instrução) {
        String opcode = instrução.substring(0, 2);
        String valor = instrução.substring(2, 6);
        int numeroInt = Integer.parseInt(valor, 16); // Assume que valor é hexadecimal
        int tipo = 3; // Todas as instruções listadas são do tipo 3
        System.out.println("Opcode: " + opcode);
        
        switch(opcode) {
			case "18": // ADD m, soma em A o valor de m
                A.valor += numeroInt;
                atualizar(instrução, tipo);
                break;
			//ADDR
			//AND
			//CLEAR
			
			// case "28":	COMP m, 
        	// 	tipo = 3;
        	// 	atualizar(instrução, tipo);
        	// 	break;

			//COMPR
			//DIV
			//DIVR
			case "3C": // J m, PC recebe o valor de m
                PC.valor = numeroInt;
                atualizar(instrução, tipo);
                break;
			// case "30":	//JEQ m, PC recebe o valor de m se CC receber valor =
        	// 	tipo = 3;
        	// 	atualizar(instrução, tipo);
        	// 	break;

			//JGT
			case "38": // JLT m, PC recebe o valor de m se CC for menor que
                // Implementação simplificada: assume condição
                atualizar(instrução, tipo);
                break;
			 case "48": // JSUB m, carrega em L o valor de PC e atribui a PC o valor de numeroInt
                L.valor = PC.valor;
                PC.valor = numeroInt;
                atualizar(instrução, tipo);
                break;
            case "00": // LDA, Carrega o valor da memória em A
                A.setValor(numeroInt);
                System.out.println("valor: " + A.valor);
                atualizar(instrução, tipo);
                break;
			//LDB
			case "50": // LDCH m, A (o bit mais importante) recebe o valor de m
                // Implementação simplificada
                atualizar(instrução, tipo);
                break;

			// case "08":	// LDL m
        	// 	tipo = 3;
        	// 	atualizar(instrução, tipo);
        	// 	break;

			//LDS
			//LDT

			// case "04":	//LDX m
        	// 	tipo = 3;
        	// 	atualizar(instrução, tipo);
        	// 	break;

			//MUL
			//MULR
			//OR
			//RMO

			case "4C": // RSUB m, PC recebe o valor de L
                PC.valor = L.valor;
                atualizar(instrução, tipo);
                break;

			//SHIFTL
			//SHIFTR

            case "0C": // STA, salva o valor de A na memória
                atualizar(instrução, tipo);
                pilha.memória[contador2] = String.valueOf(A.valor);
                contador2++;
                break;
			
			//STB
			case "54": // STCH m, m recebe A (o bit mais importante)
                // Implementação simplificada: assume que é para armazenar em memória
                atualizar(instrução, tipo);
                break;
            case "14": // STL m, carrega o valor do registrador L na memória
                pilha.memória[contador2] = String.valueOf(L.valor);
                atualizar(instrução, tipo);
                break;

			//STS
			//STT
			//STX

            case "1C": // SUB m
                A.valor -= numeroInt;
                atualizar(instrução, tipo);
                break;

			//SUBR

            case "2C": // TIX m
                // Implementação simplificada: incrementa X e compara
                X.valor++;
                if (X.valor == numeroInt) {
                    // Setar flag de igualdade se necessário
                }
                atualizar(instrução, tipo);
                break;
			
			//TIXR

            default:
                System.out.println("Opcode não reconhecido: " + opcode);
                break;
        }
    }
    
    public static void atualizar(String instrução, int tipo) {
        pilha.memória[pilha.contador] = instrução;
        PC.valor += (1 * tipo);
        pilha.contador++;
    }
}

