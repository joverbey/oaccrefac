static int foo();

int main() {

	int a[foo()];
	for (int i = 0; i < foo(); i++) { /*<<<<< 6,1,9,3,2,fail*/
		a[i] = 0;
	}

}

int foo() {
	return 100;
}
