
int main() {
	int i;
	char a[10];
	char b[10];
    for (int i = 0; i < 10; i++) { /*<<<<< 6,5,9,6,3,pass*/
        a[i] = b[i];
        i++;
    }
    i = 0;
    return i;
}
