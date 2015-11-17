/*
 * This test demonstrates that a refactoring can't happen if the loop is unrolled more
 * times than it is ran.
 */
int main() {

	char a[10];
    for (int i = 0; i < 10; i++) /*<<<<< 12,5,13,21,11,fail*/
        a[i] = '\0';
    return 0;
}
