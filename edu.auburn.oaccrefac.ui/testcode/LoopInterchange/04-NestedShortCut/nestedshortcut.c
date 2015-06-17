
static int foo();

int main() {

	for (int i = 0; i < 100; i++) /*<<<<< 6,1,9,23,2,pass*/
		for (int j = 0; j < 200; j=j+1)
			for (int k = 0; k < 300; k+=1)
				foo();

	return 0;
}

int foo() {
	return 0;
}
