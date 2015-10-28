
int main() {

	int a = 0;
	int b = 10;

	for (int i = 0; i < 100; i++) { /*<<<<< 7,5,12,1,fail */
		a = 10;
		a++;
		b = a;
	}

	for (int i = 0; i < 100; i+=1) {
		a = 20;
		a++;
		b = 0 + a;
	}


}
