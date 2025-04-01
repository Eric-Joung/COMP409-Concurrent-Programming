#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <omp.h>

#define NUM_THREADS 8

typedef struct {
    int* data; // Pointer to the array of integers
    unsigned int length; // Number of elements currently in the array
    unsigned int capacity; // Maximum number of elements the array can hold
} IntArray;

typedef struct {
    int** data;
    int rows;
    int cols;
} IntMatrix;

typedef struct {
    IntArray rowptr;
    IntArray cols;
    IntArray values;
} CSR;

void IntArray_append(IntArray* array, int value) {
    if (array->length >= array->capacity) {
        fprintf(stderr, "Array capacity exceeded\n");
        return;
    }
    array->data[array->length] = value;
    array->length++;
}

void IntArray_addAt(IntArray* array, int index, int value) {
    if (index >= array->capacity) {
        fprintf(stderr, "Index out of bounds\n");
        return;
    }
    array->data[index] = value;
    // This has obvious flaws, but for the sake of this assignment, ignore
    if (index >= array->length) {
        array->length = index + 1;
    }
}

IntMatrix* allocateIntMatrix(int rows, int cols, int p) {

    IntMatrix* matrix = (IntMatrix*)malloc(sizeof(IntMatrix));

    if (matrix == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        return NULL;
    }

    matrix->rows = rows;
    matrix->cols = cols;

    matrix->data = (int**)malloc(rows * sizeof(int*));
    if (matrix->data == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        free(matrix);
        return NULL;
    }

    for (int i = 0; i < rows; i++) {
        matrix->data[i] = (int*)malloc(cols * sizeof(int));
        if (matrix->data[i] == NULL) {
            fprintf(stderr, "Memory allocation failed\n");
            for (int j = 0; j < i; j++) {
                free(matrix->data[j]);
            }
            free(matrix->data);
            free(matrix);
            return NULL;
        }
    }

    return matrix;
}

void fillIntMatrixSequential(IntMatrix* matrix, int p) {
    // Fill the matrix with random integers between 0 and 1
    for (int i = 0; i < matrix->rows; i++) {
        for (int j = 0; j < matrix->cols; j++) {
            if (rand() % 100 < p) { // p is a percentage, so we check if the random number is less than p
                matrix->data[i][j] = 0;
            } else {
                matrix->data[i][j] = 1;
            }
        }
    }
}

void fillIntMatrixConcurrent(int id, int numThreads, IntMatrix* matrix, int p) {
    // Fill the matrix with random integers between 0 and 1
    for (int i = id; i < matrix->rows; i += numThreads) {
        for (int j = 0; j < matrix->cols; j++) {
            if (rand() % 100 < p) { // p is a percentage, so we check if the random number is less than p
                matrix->data[i][j] = 0;
            } else {
                matrix->data[i][j] = 1;
            }
        }
    }
}

void freeIntMatrix(IntMatrix* matrix) {
    for (int i = 0; i < matrix->rows; i++) {
        free(matrix->data[i]);
    }
    free(matrix->data);
    free(matrix);
}

void printIntMatrix(IntMatrix* matrix) {
    for (int i = 0; i < matrix->rows; i++) {
        for (int j = 0; j < matrix->cols; j++) {
            printf("%d ", matrix->data[i][j]);
        }
        printf("\n");
    }
}

CSR* createCSR(int nonZeros, int rows) {
    CSR* csr = (CSR*)malloc(sizeof(CSR));
    if (csr == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        return NULL;
    }

    csr->rowptr = (IntArray){.data = (int*)malloc((rows + 1) * sizeof(int)), .length = 0, .capacity = rows + 1};
    if (csr->rowptr.data == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        free(csr);
        return NULL;
    }

    csr->cols = (IntArray){.data = (int*)malloc(nonZeros * sizeof(int)), .length = 0, .capacity = nonZeros};
    if (csr->cols.data == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        free(csr->rowptr.data);
        free(csr);
        return NULL;
    }

    csr->values = (IntArray){.data = (int*)malloc(nonZeros * sizeof(int)), .length = 0, .capacity = nonZeros};
    if (csr->values.data == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        free(csr->rowptr.data);
        free(csr->cols.data);
        free(csr);
        return NULL;
    }
    
    return csr;
}

void printCSR(CSR* csr) {
    printf("Row pointers: ");
    for (int i = 0; i < csr->rowptr.capacity; i++) {
        printf("%d ", csr->rowptr.data[i]);
    }
    printf("\nColumn indices: ");
    for (int i = 0; i < csr->cols.capacity; i++) {
        printf("%d ", csr->cols.data[i]);
    }
    printf("\nValues: ");
    for (int i = 0; i < csr->values.capacity; i++) {
        printf("%d ", csr->values.data[i]);
    }
    printf("\n\n");
}

void freeCSR(CSR* csr) {
    free(csr->rowptr.data);
    free(csr->cols.data);
    free(csr->values.data);
    free(csr);
}

void countNonZeros(IntMatrix* matrix, int* nonZeros) {
    *nonZeros = 0;
    for (int i = 0; i < matrix->rows; i++) {
        for (int j = 0; j < matrix->cols; j++) {
            if (matrix->data[i][j] != 0) {
                (*nonZeros)++;
            }
        }
    }
}

void convertToCSRSequential(IntMatrix* matrix, CSR* csr, int nonZeros) {
    int nonZeroCounter = 0;
    IntArray_append(&(csr->rowptr), 0); // Initialize the first row pointer

    for (int i = 0; i < matrix->rows; i++) {
        for (int j = 0; j < matrix->cols; j++) {
            if (matrix->data[i][j] != 0) {
                IntArray_append(&(csr->cols), j);
                IntArray_append(&(csr->values), matrix->data[i][j]);
                nonZeroCounter++;
            }
        }
        IntArray_append(&(csr->rowptr), nonZeroCounter); // Update the row pointer for the next row
    }
}

void rowptrCSRConcurrent(int id, int numThreads, IntMatrix* matrix, CSR* csr) {
    // Fill the row pointer array concurrently
    for (int i = id; i < matrix->rows; i += numThreads) {
        int localNonZeroCounter = 0;
        for (int j = 0; j < matrix->cols; j++) {
            if (matrix->data[i][j] != 0) {
                localNonZeroCounter++;
            }
        }
        IntArray_addAt(&(csr->rowptr), i + 1, localNonZeroCounter); // Update the row pointer for the next row
    }    
}

void convertToCSRConcurrent(int id, int numThreads, IntMatrix* matrix, CSR* csr, int nonZeros) {
    for (int i = id; i < matrix->rows; i += numThreads) {
        int index = csr->rowptr.data[i]; // Get the starting index for this row
        for (int j = 0; j < matrix->cols; j++) {
            if (matrix->data[i][j] != 0) {
                IntArray_addAt(&(csr->cols), index, j);
                IntArray_addAt(&(csr->values), index, matrix->data[i][j]);
                index++;
            }
        }
    }
}

int main(int argc, char *argv[]) {
    // Parameters
    int n; // array dimension
    int s; // random seed
    float p = 0.05; // probability of 0

    // Check if the correct number of arguments is provided
    if (argc != 3) {
        fprintf(stderr, "Usage: %s <array_dimension> <random_seed>\n", argv[0]);
        return 1;
    }
    // Parse command line arguments
    n = atoi(argv[1]);
    s = atoi(argv[2]);

    srand(s); // Seed the random number generator

    /* Sequential Exectution */
    IntMatrix* matrixSequential = allocateIntMatrix(n, n, p * 100);
    if (matrixSequential == NULL) {
        return 1;
    }
    clock_t startMatrixSequential = clock();
    fillIntMatrixSequential(matrixSequential, p * 100);
    clock_t endMatrixSequential = clock();
    double time_spent_matrix_sequential = (double)(endMatrixSequential - startMatrixSequential) / CLOCKS_PER_SEC;

    printf("Matrix filled sequentially in %f  ms\n", time_spent_matrix_sequential * 1000);
    printIntMatrix(matrixSequential);

    int nonZerosSequential = 0;
    countNonZeros(matrixSequential, &nonZerosSequential);
    printf("Number of non-zero elements: %d\n", nonZerosSequential);

    CSR* csrSequential = createCSR(nonZerosSequential, n);
    if (csrSequential == NULL) {
        freeIntMatrix(matrixSequential);
        return 1;
    }

    clock_t startSequential = clock();
    convertToCSRSequential(matrixSequential, csrSequential, nonZerosSequential);
    clock_t endSequential = clock();
    double time_spent_CSR_sequential = (double)(endSequential - startSequential) / CLOCKS_PER_SEC;
    printf("Time taken to convert to CSR format sequentially: %f ms\n", time_spent_CSR_sequential * 1000);

    printf("Total time spent in sequential execution: %f ms\n", (time_spent_matrix_sequential + time_spent_CSR_sequential) * 1000);
    
    printCSR(csrSequential);

    // Free allocated memory for sequential execution
    freeIntMatrix(matrixSequential);
    freeCSR(csrSequential);

    /* Concurrent execution */
    // parallel parameters
    int numThreads;
    omp_set_num_threads(NUM_THREADS);

    IntMatrix* matrixConcurrent = allocateIntMatrix(n, n, p * 100);
    if (matrixConcurrent == NULL) {
        return 1;
    }

    double startMatrixConcurrent = omp_get_wtime();
    # pragma omp parallel
    {
        int id = omp_get_thread_num();
        int nthrds = omp_get_num_threads();
        if (id == 0) numThreads = nthrds; // Get actual number of threads
        fillIntMatrixConcurrent(id, nthrds, matrixConcurrent, p * 100);
    }
    double endMatrixConcurrent = omp_get_wtime();
    double time_spent_matrix_concurrent = endMatrixConcurrent - startMatrixConcurrent;
    printf("Matrix filled concurrently using %d threads in %f ms\n", numThreads, time_spent_matrix_concurrent * 1000);
    printIntMatrix(matrixConcurrent);

    int nonZerosConcurrent = 0;
    countNonZeros(matrixConcurrent, &nonZerosConcurrent);
    printf("Number of non-zero elements: %d\n", nonZerosConcurrent);

    CSR* csrConcurrent = createCSR(nonZerosConcurrent, n);
    if (csrConcurrent == NULL) {
        freeIntMatrix(matrixConcurrent);
        return 1;
    }


    // Parallelize the conversion to CSR format using OpenMP
    IntArray_append(&(csrConcurrent->rowptr), 0); // Initialize the first row pointer
    double startConcurrent = omp_get_wtime();
    # pragma omp parallel
    {
        int id = omp_get_thread_num();
        int nthrds = omp_get_num_threads();
        if (id == 0) numThreads = nthrds; // Get actual number of threads

        // Fill the row pointer array concurrently
        rowptrCSRConcurrent(id, nthrds, matrixConcurrent, csrConcurrent);

        # pragma omp barrier // Ensure all threads have completed filling the row pointer array

        # pragma omp single
        {
            for (int i = 1; i < csrConcurrent->rowptr.capacity; i++) {
                csrConcurrent->rowptr.data[i] = csrConcurrent->rowptr.data[i - 1] + csrConcurrent->rowptr.data[i];
            }
        }

        convertToCSRConcurrent(id, nthrds, matrixConcurrent, csrConcurrent, nonZerosConcurrent);
    }
    double endConcurrent = omp_get_wtime();
    double time_spent_CSR_concurrent = (double)(endConcurrent - startConcurrent) / CLOCKS_PER_SEC;
    printf("Time taken to convert to CSR format concurrently using %d threads: %f ms\n", numThreads, time_spent_CSR_concurrent * 1000); // Convert to milliseconds

    printf("Total time spent in concurrent execution: %f ms\n", (time_spent_matrix_concurrent + time_spent_CSR_concurrent) * 1000);
    printCSR(csrConcurrent);
    
    // Free allocated memory for concurrent execution
    freeIntMatrix(matrixConcurrent);
    freeCSR(csrConcurrent);

    return 0;
}