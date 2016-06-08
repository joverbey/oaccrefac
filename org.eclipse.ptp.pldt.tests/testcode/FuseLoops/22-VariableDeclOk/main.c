int main() {
    int a, b, c, d;
    for (int i = 0; i < 100; i++) { /*<<<<< 3,5,3,34,pass*/
    	int c = 10;
        a = c;
        b = 6;
    }
    for (int i = 0; i < 100; i++) {
        b = 7;
        d = 8;
    }
}
