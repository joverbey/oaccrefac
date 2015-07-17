int main() {
    int a, b, d;

    for (int i = 0; i < 100; i++) { /*<<<<< 3,5,3,34,pass*/
        a = 5;
        int c = 5;
        d = c;
    }

    for (int i = 0; i < 100; i++) {
        b = 7;
        int c = 10;
        d = c + 5;
    }
}
