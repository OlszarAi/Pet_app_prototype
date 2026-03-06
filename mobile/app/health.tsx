import {View,Text, StyleSheet} from 'react-native';
import { useHealth } from '../src/hooks/useHealth';

export default function HealthScreen(){
    const { data, isLoading, isError} = useHealth();

    if(isLoading){
        return(
            <View style={styles.container}>
                <Text style={styles.text}>Loading...</Text>
            </View>
        )
    }
    if(isError){
        return(
            <View style={styles.container}>
                <Text style={styles.errorText}>Error fetching health status.</Text>
            </View>
        )
    }
    return (
        <View style={styles.container}>
            <Text style={styles.text}>Health Status</Text>
            <Text style={styles.status}>{data?.status}</Text>
        </View>
    )
        
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    color: '#fff',
    fontSize: 18,
    marginBottom: 8,
  },
  status: {
    color: '#30D158', 
    fontSize: 28,
    fontWeight: 'bold',
  },
  errorText:{
    fontSize: 22,
    fontWeight: 'bold',
    color: '#F54927'
  }
});