int main() {
    int a, b;
    float d;

    for (int i = 0; i < 100; i++) { /*<<<<< 5,1,10,1,fail*/
        a = 5;
        int c = 5;
        d = c;
    }

    for (int i = 0; i < 100; i++) {
        b = 7;
        float c = 10.2;
        d = c + 5;
    }
}
