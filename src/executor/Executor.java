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
    static Memoria pilha = new Memoria();
    static int contador2 = 100;

    public static void main(String[] args) {
        try {
            // Cria um objeto File representando o arquivo "input.txt"
            File file = new File("/home/penedo/Downloads/Estudos/Universidade/PS/PS_TRABALHO/src/executor/input.txt");
            
            // Cria um FileInputStream para ler bytes do arquivo
            FileInputStream fis = new FileInputStream(file);
            
            // Cria um BufferedReader para ler linhas de texto do FileInputStream
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String linha;
            // Lê cada linha do arquivo até o final
            while ((linha = br.readLine()) != null) {
                // Processa cada linha do arquivo
                String instrucaoHexa = linha.trim(); // Remove espaços em branco
               
                // Decodificação e execução da instrução
                executarInstrucao(instrucaoHexa); // Executa a instrução
                System.out.println("Valor de A: " + A.number);
                System.out.println("Valor de PC: " + PC.number);
            }
            
            // Fecha o BufferedReader
            br.close();
        } catch (IOException e) {
            // Trata exceções de E/S (leitura do arquivo)
            e.printStackTrace();
        }
        pilha.printAll();
    }

    // Método para decodificar e executar a instrução
    public static void executarInstrucao(String instrucao) {
        String opcode = instrucao.substring(0, 2);
        String valor = instrucao.substring(2, 6);
        int numeroInt = Integer.parseInt(valor, 16); // Assume que valor é hexadecimal
        int tipo = 3; // Todas as instruções listadas são do tipo 3
        System.out.println("Opcode: " + opcode);
        
        switch(opcode) {
			case "18": // ADD m, soma em A o valor de m
                A.number += numeroInt;
                atualizar(instrucao, tipo);
                break;
			//ADDR
			//AND
			//CLEAR
			
			// case "28":	COMP m, 
        	// 	tipo = 3;
        	// 	atualizar(instrucao, tipo);
        	// 	break;

			//COMPR
			//DIV
			//DIVR
			case "3C": // J m, PC recebe o valor de m
                PC.number = numeroInt;
                atualizar(instrucao, tipo);
                break;
			// case "30":	//JEQ m, PC recebe o valor de m se CC receber valor =
        	// 	tipo = 3;
        	// 	atualizar(instrucao, tipo);
        	// 	break;

			//JGT
			case "38": // JLT m, PC recebe o valor de m se CC for menor que
                // Implementação simplificada: assume condição
                atualizar(instrucao, tipo);
                break;
			 case "48": // JSUB m, carrega em L o valor de PC e atribui a PC o valor de numeroInt
                L.number = PC.number;
                PC.number = numeroInt;
                atualizar(instrucao, tipo);
                break;
            case "00": // LDA, Carrega o valor da memoria em A
                A.setNumber(numeroInt);
                System.out.println("valor: " + A.number);
                atualizar(instrucao, tipo);
                break;
			//LDB
			case "50": // LDCH m, A (o bit mais importante) recebe o valor de m
                // Implementação simplificada
                atualizar(instrucao, tipo);
                break;

			// case "08":	// LDL m
        	// 	tipo = 3;
        	// 	atualizar(instrucao, tipo);
        	// 	break;

			//LDS
			//LDT

			// case "04":	//LDX m
        	// 	tipo = 3;
        	// 	atualizar(instrucao, tipo);
        	// 	break;

			//MUL
			//MULR
			//OR
			//RMO

			case "4C": // RSUB m, PC recebe o valor de L
                PC.number = L.number;
                atualizar(instrucao, tipo);
                break;

			//SHIFTL
			//SHIFTR

            case "0C": // STA, salva o valor de A na memoria
                atualizar(instrucao, tipo);
                pilha.memory[contador2] = String.valueOf(A.number);
                contador2++;
                break;
			
			//STB
			case "54": // STCH m, m recebe A (o bit mais importante)
                // Implementação simplificada: assume que é para armazenar em memoria
                atualizar(instrucao, tipo);
                break;
            case "14": // STL m, carrega o valor do registrador L na memoria
                pilha.memory[contador2] = String.valueOf(L.number);
                atualizar(instrucao, tipo);
                break;

			//STS
			//STT
			//STX

            case "1C": // SUB m
                A.number -= numeroInt;
                atualizar(instrucao, tipo);
                break;

			//SUBR

            case "2C": // TIX m
                // Implementação simplificada: incrementa X e compara
                X.number++;
                if (X.number == numeroInt) {
                    // Setar flag de igualdade se necessário
                }
                atualizar(instrucao, tipo);
                break;
			
			//TIXR

            default:
                System.out.println("Opcode não reconhecido: " + opcode);
                break;
        }
    }
    
    public static void atualizar(String instrucao, int tipo) {
        pilha.memory[pilha.counter] = instrucao;
        PC.number += (1 * tipo);
        pilha.counter++;
    }
}

