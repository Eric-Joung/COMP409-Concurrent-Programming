#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <omp.h>

#define STATES 3
#define DIGITS 10

// Transition table for the DFA
int transitionTable[STATES][DIGITS] = {
    // 0  1  2  3  4  5  6  7  8  9   // input digits
      {0, 2, 1, 2, 2, 1, 1, 2, 1, 0}, // state 0 (ok state)
      {2, 0, 2, 0, 1, 2, 2, 0, 1, 2}, // state left (error state)
      {2, 1, 0, 2, 1, 0, 0, 0, 1, 2}  // state right (error state)
};

void generateRandomString(char *str, int length) {
    for (int i = 0; i < length; i++) {
        str[i] = '0' + (rand() % 10); // Generate a random lowercase letter
    }
    str[length] = '\0'; // Null-terminate the string
}

void processSegment(char *str, int start, int end, int* results) {
    for (int i = 0; i < STATES; i++) {
        int state = i; // Start in state i
        for (int j = start; j < end; j++) {
            int input = str[j] - '0'; // Convert char to int
            state = transitionTable[state][input]; // Update state based on transition table
        }
        results[i] = state;
    }
}

int main(int argc, char *argv[]) {
    // Parameters
    int n; // size of string
    int t; // number of threads
    int s = 2; // random seed

    srand(s); // Seed the random number generator

    // Check if the correct number of arguments is provided
    if (argc != 3) {
        fprintf(stderr, "Usage: %s <array_dimension> <random_seed>\n", argv[0]);
        return 1;
    }

    // Parse command line arguments
    t = atoi(argv[1]);
    n = atoi(argv[2]);

    // Check if n > t
    if (n <= t) {
        fprintf(stderr, "Error: n must be greater than t\n");
        return 1;
    }

    // Allocate memory for the string
    char *str = (char *)malloc((n+1) * sizeof(char));
    if (str == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        return 1;
    }
    generateRandomString(str, n);
    printf("Generated string: %s\n", str);

    int segmentSize = n / t; // Size of each segment
    int remainder = n % t; // Remainder for the last segment

    int numThreads;
    int stateMappings[t][STATES]; // Store final states for each thread depending on starting state

    double start = omp_get_wtime();
    # pragma omp parallel num_threads(t)
    {
        int id = omp_get_thread_num();
        int nthrds = omp_get_num_threads();
        if (id == 0) numThreads = nthrds; // Get actual number of threads

        // Each thread processes its segment of the string
        int start = id * segmentSize;
        int end = (id + 1) * segmentSize;
        if (id == nthrds - 1) {
            end += remainder; // Last thread gets the remainder
        }

        processSegment(str, start, end, stateMappings[id]);
    }

    // Sequential resolution of the final states
    int finalState = 0;
    for (int i = 0; i < numThreads; i++) {
        finalState = stateMappings[i][finalState];
    }

    double end = omp_get_wtime();
    double time_spent = end - start;

    if (finalState == 0) {
        printf("true\n");
    } else {
        printf("false\n");
    }

    printf("Time taken for %d threads: %f ms\n", numThreads, time_spent * 1000);

    // Free memory
    free(str);

    return 0;
}