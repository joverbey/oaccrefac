int main() {
    int a[100], b, c;
    for (int i = 0; i < 100; i++) { /*<<<<< 3,1,6,4,fail*/
        a[i] = b;
        c = a[i + 1];
    }
}
