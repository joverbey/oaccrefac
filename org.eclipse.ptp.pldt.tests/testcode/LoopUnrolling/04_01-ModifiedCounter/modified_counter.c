
int main() {

	char a[10];
	char b[10];
	int d = 0;
    for (int i = 0; i < 10; i++) { /*<<<<< 6,5,9,6,3,fail*/
        i++;
        a[i] = b[i];
        d++;
    }
    return 0;
}
