int main() {

	for (int i = 0; i < 100; i++) /*<<<<< 3,1,7,1,2,pass*/
		for (int j = 0; j < 200; j=j+1)
			for (int k = 0; k < 300; k+=1)
				;

	return 0;
}
