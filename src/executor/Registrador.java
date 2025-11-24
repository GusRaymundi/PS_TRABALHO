package executor;

public class Registrador{
	protected int number;
	private String address;

	Registrador(String address){
		this.number = 0;
		this.address = address;
	}	

	public int getNumber(){
		return this.number;
	}

	public String getAddress(){
		return this.address;
	}

	public void setNumber(int number){
		this.number = number;
	}

	/*
	public void testRegistrador(){
		getNumber();
		getAddress();
	}
	*/
}