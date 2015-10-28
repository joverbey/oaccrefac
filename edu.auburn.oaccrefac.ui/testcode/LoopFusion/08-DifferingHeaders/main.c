
int main() {

	int a = 0;

	for (int i = 0; i < 100; i++) { /*<<<<< 6,5,9,1,fail */
		a = 10;
	}

	for (int i = 0; i < 200; i++) {
		a = 20;
	}


}
