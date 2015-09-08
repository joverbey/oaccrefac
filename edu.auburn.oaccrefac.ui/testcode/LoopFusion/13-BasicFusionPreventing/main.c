int main() {

	int a[10], b[10], c, d;

	for (int i = 0; i < 10; i++) { /*<<<<< 5,1,9,1,fail */
		a[i] = b[i] + c;
	}

	for (int i = 0; i < 10; i++) {
		b[i] = a[i + 1] + d;
	}

}
