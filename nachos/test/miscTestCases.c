#include "readline.h"

/**
*TODO:
1) Single file read/writes with 
	1) buffer smaller than given size
	2) buffer bigger than given size
	3) buffer smaller than amount to read/write
	4) buffer bigger than amount to read/write
	5) Opening multiple files
	6) Reading/writing multiple files simultaneously
	7) Opening too many files
	8) Opening and closing files (fill 16 spots)
	8) Deleting a file
*/

int createFile(char* fileName){
	int fd = creat(fileName);
	printf("The file descriptor is %d\n", fd);
	return fd;
}

int openFile(char* fileName){
	int fd = open(fileName);
	printf("The file descriptor is %d\n", fd);
	return fd;
}

/**
 * Test that a file can be opened 
 */
void test_createOpenTest(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int fdOpen = openFile(filename);
	if (fdCreate != fdOpen){
		printf("-------createOpenTest TEST FAILED-------\n The opened file descriptor and the existing file descriptor should match");
	} else {
		printf("-------createOpenTest TEST PASSED-------\n");
		close(fdCreate);
		unlink(filename);
	}
}

/**
 * Test that a file can be opened and number of file pointers is okay
 */
void test_openingTwoFiles(){
	char* filename1 = "AAnewFile.txt";
	char* filename2 = "AAoldFile.txt";
	int fdCreate1 = createFile(filename1);
	int fdCreate2 = createFile(filename2);
	int fdOpen2 = openFile(filename2);
	if (fdCreate1 != fdCreate2 && fdCreate1 != fdOpen2 && fdCreate2 == fdOpen2) {
		printf("-------openingTwoFiles TEST PASSED-------\n");
		close(fdCreate1);
		unlink(filename1);
		close(fdCreate2);
		unlink(filename2);
	}
	else {
		printf("-------openingTwoFiles TEST FAILED-------\n The opened file descriptor and the existing file descriptor should NOT match");
	}
}

/**
 * Test that opening a file that does not exist failed
 */
void test_openingDNEFile(){
	char* filename1 = "DNE.txt";
	int fdOpen = openFile(filename1);
	if (fdOpen == -1) {
		printf("-------openingDNEFile TEST PASSED-------\n");
	}
	else {
		printf("-------openingDNEFile TEST FAILED-------\n The opened file descriptor  should NOT exist");
	}
}

/**
 * Test writing to a file
 */
void test_createWriteToFile(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, filename, 20); 
	if (writtenBytes == 13) {
		printf("-------test_createWriteToFile TEST PASSED-------\n");
	}
	else {
		printf("-------test_createWriteToFile TEST FAILED-------\n Error writing to file");
	}
	close(fdCreate);
	unlink(filename);
}
/**
 * Test writing to a file
 */
void test_createWriteToFileEmpty(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, "", 20); 
	if (writtenBytes == 0) {
		printf("-------test_createWriteToFileEmpty TEST PASSED-------\n");
	}
	else {
		printf("-------test_createWriteToFileEmpty TEST FAILED-------\n Error writing to file");
	}
	close(fdCreate);
	unlink(filename);
}

/**
 * Test writing null to a file
 */
void test_createWriteToFileNull(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, 0, 20); 
	if (writtenBytes == -1) {
		printf("-------test_createWriteToFileNull TEST PASSED-------\n");
	}
	else {
		printf("-------test_createWriteToFileNull TEST FAILED-------\n Error writing to file");
	}
	close(fdCreate);
	unlink(filename);
}

/**
 * Test writing null to a file
 */
void test_createReadToFileNull(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, "asdfasdfasdfasdfasdfadsf", 20); 
	char* buf = null;
	int readBytes = read(fdCreate, buf, 20);
	if (readBytes == -1) {
		printf("-------test_createReadToFileNull TEST PASSED-------\n");
	}
	else {
		printf("-------test_createReadToFileNull TEST FAILED-------\n Error writing to file");
	}
	close(fdCreate);
	unlink(filename);
}

void test_createReadToFileOne(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, "asdfasdfasdfasdfasdfadsf", 20); 
	close(fdCreate);
	int fdOpen = openFile(filename);
	char buf[5];
	int readBytes = read(fdOpen, buf, 20);
	if (readBytes == 1) {
		printf("-------test_createReadToFileOne TEST PASSED-------\n");
	}
	else {
		printf("-------test_createReadToFileOne TEST FAILED-------\n Error writing to file");
	}
	close(fdCreate);
	unlink(filename);
}

void test_createWriteToFileCutoff(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", 20); 
	if (writtenBytes == 20) {
		printf("-------test_createWriteToFileCutoff TEST PASSED-------\n");
	}
	else {
		printf("-------test_createWriteToFileCutoff TEST FAILED-------\n Error writing to file");
	}
	close(fdCreate);
		unlink(filename);
}
void test_createWriteToFileMultipleWrites(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	writtenBytes +=  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	writtenBytes +=  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	if (writtenBytes == 42) {
		printf("-------test_createWriteToFileCutoff TEST PASSED-------\n");
	}
	else {
		printf("-------test_createWriteToFileCutoff TEST FAILED-------\n Error writing to file");
	}
	close(fdCreate);
		unlink(filename);
}

void test_createWriteToRead(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	writtenBytes +=  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	writtenBytes +=  write(fdCreate, "aaaaaaaaaaaaa\n", 20);
	close(fdCreate);

	char buf[50];
	int fdOpen = openFile(filename);
	int bytesRead = read(fdOpen, buf, 50);
	if (bytesRead == 42) {
		printf("-------test_createWriteToFileCutoff TEST PASSED-------\n");
		printf(buf);
	}
	else {
		printf("-------test_createWriteToFileCutoff TEST FAILED-------\n Error writing to file");
		printf(buf);
	}
	close(fdCreate);
		unlink(filename);
}

void test_createWriteToReadSome(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	writtenBytes +=  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	writtenBytes +=  write(fdCreate, "aaaaaaaaaaaaa\n", 20);
	close(fdCreate);

	char buf[20];
	int fdOpen = openFile(filename);
	int bytesRead = read(fdOpen, buf, 20);
	if (bytesRead == 20) {
		printf("-------test_createWriteToFileCutoff TEST PASSED-------\n");
		printf(buf);
	}
	else {
		printf("-------test_createWriteToFileCutoff TEST FAILED-------\n Error writing to file");
		printf(buf);
	}
	close(fdCreate);
		unlink(filename);
}

void test_createWriteToReadWantTooMuch(){
	char* filename = "AAnewFile.txt";
	int fdCreate = createFile(filename);
	int writtenBytes =  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	writtenBytes +=  write(fdCreate, "aaaaaaaaaaaaa\n", 20); 
	writtenBytes +=  write(fdCreate, "aaaaaaaaaaaaa\n", 20);
	close(fdCreate);

	char buf[20];
	int fdOpen = openFile(filename);
	int bytesRead = read(fdOpen, buf, 50);
	if (bytesRead == 20) {
		printf("-------test_createWriteToFileCutoff TEST PASSED-------\n");
		printf(buf);
	}
	else {
		printf("-------test_createWriteToFileCutoff TEST FAILED-------Error writing to file\n");
		printf(buf);
	}
	close(fdCreate);
		unlink(filename);
}

void test_create16Files(){

	int sum = 0;
	int i = 0;
	char* filename = "AAnewFileA.txt";
	filename[9] = 'A'-1; 
	for (i = 0; i < 16; i++){
		filename[9] = filename[9]+1; 
		int fdCreate = createFile(filename);
		if (fdCreate < 2) {
			printf("-------test_create16Files-------\n Could not create file %s", filename);
		} else {
		sum += fdCreate;
		}
	}
	if (sum == 119) {
		printf("-------test_create16Files TEST PASSED------\n");
	} else {
		printf("-------test_create16Files TEST FAILED------ The sum was: %d\n", sum);
	}
	
	
	filename[9] = 'A'-1; 
	for (i = 0; i < 16; i++){
		filename[9] = filename[9]+1; 
		close(i+2);
		unlink(filename);
	}
	
	
}


void test_create16FilesWriteCloseUnlink(){

	int sum = 0;
	int i = 0;
	// create
	char* filename = "AAnewFileA.txt";
	filename[9] = 'A'-1; 
	for (i = 0; i < 16; i++){
		filename[9] = filename[9]+1; 
		int fdCreate = createFile(filename);
		if (fdCreate < 2) {
			printf("-------test_create16FilesWriteCloseUnlink------- Could not create file %s\n", filename);
		} else {
		sum += fdCreate;
		}
	}
	// write
	filename[9] = 'A'-1; 
	for (i = 0; i < 16; i++){
		filename[9] = filename[9]+1; 
		int fdOpen = openFile(filename);
		if (fdOpen > 1) {
			int writeFail = write(fdOpen, "aaaaaaaaaaaaa\n", 20);
			if (writeFail < 0){
				printf("-------test_create16FilesWriteCloseUnlink TEST FAILED------ Could not write to file %s\n", filename);
			}
		} else {
			if (i < 14) {
				printf("-------test_create16FilesWriteCloseUnlink TEST FAILED------ Could not open to file %s with fd %d\n", filename, fdOpen);
			}
		}
	}
	if (sum == 119) {
		printf("-------test_create16FilesWriteCloseUnlink TEST PASSED------\n");
	} else {
		printf("-------test_create16FilesWriteCloseUnlink TEST FAILED------ The sum was: %d\n", sum);
	}
	
	filename[9] = 'A'-1; 
	for (i = 0; i < 16; i++){
		filename[9] = filename[9]+1; 
		close(i+2);
		unlink(filename);
	}
	
}


void test_create32Files(){
printf("-------STARTING test_create32Files TEST PASSED-------\n");
	int failed = 0;
	int sum = 0;
	int i = 0;
	char* filename = "AAnewFileA.txt";
	filename[9] = 'A'-1; 
	//filename[9] = 'A'-1; 
	for (i = 0; i < 26; i++){
		int actualCounter = (i % 14) + 2;
		printf("-------test_create32Files actual counter is %d\n", actualCounter);
		if (i > 13){
		printf("-------test_create32Files closing fd %d\n", actualCounter);
			close(actualCounter);
		}
		filename[9] = filename[9]+1; 
		int fdCreate = createFile(filename);
		if (fdCreate < 2) {
			printf("-------test_create32Files TEST FAILED-------\n Could not create file %s\n", filename);
			failed++;
		} else {
		sum += fdCreate;
		}
	}
	if (failed == 0){
		printf("-------test_create32Files TEST PASSED-------\n");
	} else {
		printf("-------test_create32Files TEST FAILED-------\n");
	}
	
	filename[9] = 'A'-1; 
	for (i = 0; i < 26; i++){
		filename[9] = filename[9]+1;
		int actualCounter = (i % 14) + 2;
		close(actualCounter);
		unlink(filename);
	}
	
}

void testReadline(){
char ch[10];
	readline(ch, 10);
}

void test_createCloseUnlink(){
	char* filename = "AAUnlinkZ.txt";
	int fdCreate = createFile(filename);
	close(fdCreate);
	int unlinkSuccess = unlink(filename);
	if (unlinkSuccess == 0){
		printf("-------test_createCloseUnlink TEST PASSED-------\n");
	} else {
		printf("-------test_createCloseUnlink TEST FAILED-------\n");
	}
}

void test_createUnlink(){
	char* filename = "AAUnlinkZ.txt";
	int fdCreate = createFile(filename);
	int unlinkSuccess = unlink(filename);
	if (unlinkSuccess == 0){
		printf("-------test_createUnlink TEST PASSED-------\n");
	}
}

void testAll(){
	test_createOpenTest();
	test_openingTwoFiles();
	test_openingDNEFile();
	test_createWriteToFile();
	test_createWriteToFileEmpty();
	test_createWriteToFileNull();
	test_createWriteToFileCutoff();
	test_createWriteToFileMultipleWrites();
	test_createWriteToRead();
	test_createWriteToReadSome();
//	test_createWriteToReadWantTooMuch();
	test_create16Files();
	test_create32Files();
	test_createCloseUnlink();
	test_createUnlink();
	test_create16FilesWriteCloseUnlink();
}

int main(){
	//test_createReadToFileOne();
	testAll();
}

