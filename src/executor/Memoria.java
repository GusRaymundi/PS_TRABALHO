package executor;

public class Memoria {
    protected String[] memory = new String[350];
    public int counter = 0;

    public Memoria(){
        for(int i=0; i<350; i++){
            memory[i] = "0x00";
        }
    }

    public String getMemory(){
        if(counter>0){
            return memory[counter-1];
        }
        else{
            return "Memória vazia";
        }
    }

    public void setMemory(String content){
        memory[counter] = content;
        counter++;
    }

    public void printAll(){
        for(int i=0; i<350; i++){
            System.out.println(i + ":" + memory[i]);
        }
    }

    // converter para binário no executor

}
