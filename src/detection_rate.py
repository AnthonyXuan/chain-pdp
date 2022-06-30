from cProfile import label
import math
import json
from matplotlib import pyplot as plt
from matplotlib.lines import Line2D


# Hi, in my paper \texwidth = 516.0 pt , i.e., 7.1666666666668 inches
# figsize use 1 inch as unit
tex_width = 7.17
fig_length = tex_width*0.7
fig_scale = 0.8
alpha = 0.3

def corr_rate_to_blocks(n, corruption_rate):
    temp = n*corruption_rate
    if(temp < 1.0):
        print("Warning~~: corrupt block({}) less than 1 !".format(temp))
    x = math.ceil(temp)
    return x

def detection_rate(n, c, x):
    eq1 = math.comb(n-x, c)
    eq2 = math.comb(n, c)
    detect_rate = 1 - eq1/eq2
    return detect_rate

def detection_block(bassline, n, corruption_rate):
    x = corr_rate_to_blocks(n, corruption_rate)
    # Start from c = 1
    return recurv_block(bassline, 1, n, n, x)

def recurv_block(bassline, start, stop, n, x):
    if(stop - start) == 1:
        return stop
    mid = math.floor((start + stop)/2)
    if detection_rate(n, mid, x) >= bassline:
        return recurv_block(bassline, start, mid, n, x)
    else:
        return recurv_block(bassline, mid, stop, n, x)

# 以表格的形式输出需要的块数    
def get_table(n_arr, corruption_rate_arr, bassline_arr):
    print(((4*' ').join(str(item) for item in bassline_arr)).rjust(row_len))
    for n in n_arr:
        for corruption_rate in corruption_rate_arr:
            sub_get_table(n, corruption_rate, bassline_arr)
    
def sub_get_table(n, corruption_rate, bassline_arr):
    head = 'n={},c_rate={}'.format(n, corruption_rate)
    body = ''
    for bassline in bassline_arr:
        body = body + str(detection_block(bassline, n, corruption_rate)) + ' '*4
    print(head.ljust(30) + body)

# 以文本的形式输出需要块数的段落
def get_sentence(n_arr, corruption_rate_arr, bassline_arr):
    with open('检测结果.txt','w') as fp:
        for corruption_rate in corruption_rate_arr:
            sentence = '若Space Owner的损坏率为{:.0%}，则为了达到'.format(corruption_rate,)
            temp = ['{:.1%}'.format(bassline) for bassline in bassline_arr]
            sentence_bass_rate = '， '.join(temp)
            sentence = sentence + sentence_bass_rate + '的检测准确率，需要抽查的块数分别为：'
            for n in n_arr:
                sentence = sentence + '对于{}块的文件，需要抽查'.format(n)
                temp = [str(detection_block(bassline, n, corruption_rate)) for bassline in bassline_arr]
                sentence_block = '块，'.join(temp[:-1])
                sentence_block_append = '和{}块。'.format(temp[-1])
                sentence = sentence + sentence_block + sentence_block_append
            print(sentence)
            fp.write(sentence + '\n')
    for_latex()
            
def for_latex():
    with open('检测结果.txt','r') as fp:
        context = fp.read()
        latex_context = context.replace('%', '\%')
    with open('检测结果.txt','w') as fp:
        fp.write(latex_context)
        
def gen_whole_graph(n_arr, expect_block_cnt, corruption_rate_arr):
    result = dict()
    for n in n_arr:
        result[n] = gen_sub_graph(n, expect_block_cnt, corruption_rate_arr)
    with open('graph_data.json','w') as fp:
        json.dump(result, fp)
    
def gen_sub_graph(n, expect_block_cnt, corruption_rate_arr):
    step = n // expect_block_cnt
    result = []
    result_0 = []
    result_1 = dict()
    for i in range(1, expect_block_cnt):
        result_0.append(i*step)
    
    for corruption_rate in corruption_rate_arr:
        corr_blocks = corr_rate_to_blocks(n, corruption_rate)
        temp = []           
        for c in result_0:
            temp.append(detection_rate(n, c, corr_blocks))
        result_1[corruption_rate] = temp
        
    result.append(result_0)
    result.append(result_1)
    return result

def plot_whole_graph(bassline_arr=None):
    with open('graph_data.json','r') as fp:
        result = json.load(fp)

    for key in result.keys():
        plot_sub_graph(key, result[key], bassline_arr)
    
    
def plot_sub_graph(n, data, bassline_arr=None):
    ms = 7
    marker_suite = [
        ['b-', 'g-', 'r-', 'c-', 'm-'],
        ['d', 'o', 'v', '>', 'H']
    ]
    plt.figure(figsize=(fig_length, fig_length*fig_scale))
    x = data[0]
    plt_arr = []
    name_arr = []
    x_right_limit = -1

    key_float_arr = [float(key) for key in data[1].keys()]
    key_float_arr.sort(reverse=True)
    # key is a float now
    for index, key in enumerate(key_float_arr):
        y = data[1][str(key)]   
        temp, = plt.plot(x, y, marker_suite[0][index], marker=marker_suite[1][index], ms=ms, markerfacecolor='none')
        plt_arr.append(temp)
        name_arr.append('{:.0%} corruption rate'.format(key))
        
        if bassline_arr is not None:
            if index == len(data[1].keys()) - 1:
                
                for i,item in enumerate(y):
                    print('item=',item,'bass=',bassline_arr[-1])
                    if item >= bassline_arr[-1]:
                        x_right_limit = x[i]
                        print(x_right_limit)
                        break
        
    if bassline_arr is not None:
        for bassline in bassline_arr:
            plt.axhline(bassline, ls='--')
            plt.text(15, bassline, '{}'.format(bassline))
    plt.legend(plt_arr, name_arr)
    #plt.title('{} blocks'.format(n))
    if x_right_limit != -1:
        plt.xlim(right = x_right_limit)
    plt.xlabel('Challenged blocks')
    plt.ylabel('Detection rate')
    plt.tight_layout()
    plt.grid(alpha = 0.3, ls='--')
    #plt.show()
    plt.savefig('{}-blocks-detection.svg'.format(n), format = 'svg')



#正态分布的三个概率
bassline_arr = [0.68, 0.95, 0.997]
n_arr = [200, 500, 1000, 2000, 200000]
corruption_rate_arr = [0.01, 0.05, 0.1, 0.2]

row_len = 50
expect_block_cnt = 30

#get_sentence(n_arr, corruption_rate_arr, bassline_arr)
get_table(n_arr, corruption_rate_arr, bassline_arr)
#gen_whole_graph(n_arr, expect_block_cnt, corruption_rate_arr)
#plot_whole_graph()