
int main() {

	int a = 0;
	int b = 10;

	for (int i = 0; i < 100; i++) { /*<<<<< 6,1,12,1,pass */
		a = 10;
		a++;
		b = a;
	}

	for (int j = 0; j < 100; j+=1) {
		a = 20;
		a++;
		b = 0 + a;
	}

	for (int i = 0; i < 100; i++) {
		a = 20;
		a++;
		b = 0 + a;
	}


}
