
int main() {
	int i;
	char a[10];
	char b[10];
	int d = 0;
    for (int i = 0; i < 10; i++) { /*<<<<< 7,5,11,6,3,fail*/
        a[i] = b[i];
        i++;
        d++;
    }
    i = 0;
    return i;
}
