CC  	= gcc
CXX	= g++
CFLAGS  = -std=c99 -fopenmp -O3 -g
CXXFLAGS= -fopenmp -O3 -g
INCLUDEFLAGS = -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux"
LDFLAGS = -fopenmp -lssl -lcrypto
SRC_DIR = $(PWD)
LIB_FILE = libBitcoinjHello.so
TARGETS = PoW $(LIB_FILE)

.PHONY:all
all : $(TARGETS)

PoW:main.o $(OBJS)
	$(CXX) -o $@ $^ $(LDFLAGS) 
	# rm *.d *.o 

libBitcoinjHello.so: $(OBJS)
	gcc -shared -o  $@ $^ -lssl -lcrypto
	sudo cp $(LIB_FILE) /usr/lib
	sudo ldconfig
%.o:%.c
	$(CC) $(CFLAGS) -o $@ -fPIC -c $< $(INCLUDEFLAGS) 

%.d:%.c
	@set -e; rm -f $@; $(CC) -MM $< $(INCLUDEFLAGS) > $@.$$$$; \
	sed 's,\($*\)\.o[ :]*,\1.o $@ : ,g' < $@.$$$$ > $@; \
	rm -f $@.$$$$

-include $(OBJS:.o=.d)

.PHONY:clean 
clean:
	rm -f $(TARGETS) *.o *.d *.d.*
