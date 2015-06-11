
static int foo();

int main() {

	for (int i = 0; i < 100; i++) { /*<<<<< 6,1,10,6,2,pass*/
		for (int j = 0; j < 200; j++) {
			foo();
		}
	}

	return 0;
}

int foo() {
	return 0;
}
