int __ulock_wait(uint32_t operation, void *addr, uint64_t value, uint32_t timeout_us);
int __ulock_wait2(uint32_t operation, void *addr, uint64_t value, uint64_t timeout_ns, uint64_t value2);
int __ulock_wake(uint32_t operation, void *addr, uint64_t wake_value);

// operation bits [7, 0] contain the operation code.
#define UL_COMPARE_AND_WAIT		        1
#define UL_UNFAIR_LOCK			        2
#define UL_COMPARE_AND_WAIT_SHARED	    3
#define UL_UNFAIR_LOCK64_SHARED		    4
#define UL_COMPARE_AND_WAIT64		    5
#define UL_COMPARE_AND_WAIT64_SHARED	6

// operation bits [15, 8] contain the flags for __ulock_wake
#define ULF_WAKE_ALL			        0x00000100
#define ULF_WAKE_THREAD			        0x00000200
#define ULF_WAKE_ALLOW_NON_OWNER	    0x00000400

// operation bits [23, 16] contain the flags for __ulock_wait
#define ULF_WAIT_WORKQ_DATA_CONTENTION	0x00010000
#define ULF_WAIT_CANCEL_POINT		    0x00020000
#define ULF_WAIT_ADAPTIVE_SPIN		    0x00040000

// operation bits [31, 24] contain the generic flags, which can be used with both __ulock_wait and __ulock_wake
#define ULF_NO_ERRNO			        0x01000000