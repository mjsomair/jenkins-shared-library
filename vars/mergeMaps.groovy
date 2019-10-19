// Custom Merge of 2 multi-level maps
// Takes base map and then updates values based on the diff map
// Returns resulting map that has the structure of base and values updated from diff
// Support up to 3 level deep Map:Map:Map in case we need it for future enhancements
//
// TODO Rewrite using recursion to support any number fo level (within sane limits)

def Map call(Map base = [:], Map diff = [:]) {
	def result = [:]
	// both base and diff must not be null
	if (base && diff) {
		base.each { L1key, L1value ->
			if (L1value instanceof Map) {
				result[L1key] = [:]
				L1value.each { L2key, L2value ->
					if (L2value instanceof Map) {
						result[L1key][L2key] = [:]
						L2value.each { L3key, L3value ->
							result[L1key][L2key][L3key] = diff[L1key] && diff[L1key][L2key] && diff[L1key][L2key].containsKey(L3key) ? diff[L1key][L2key][L3key] : L3value
						}
					} else {
						result[L1key][L2key] = diff[L1key] && diff[L1key].containsKey(L2key) ? diff[L1key][L2key] : L2value
					}
				}
			} else {
				result[L1key] = diff.containsKey(L1key) ? diff[L1key] : L1value
			}
		}
	} else {
		// otherwise return the base
		result = base
	}
	return result
}