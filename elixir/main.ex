defmodule FourRings do
    def wrap64(v) do
        <<result::signed-integer-64>> = <<v::integer-64>>
        result
    end

    def start(n, h) do
        IO.puts("N = #{n}")
        IO.puts("H = #{h}")

        # spawn rings with transform functions
        # keep hold of pid to "gatekeeper" functions
        neg_gk  = build_ring(n, h, fn v -> wrap64(v * 3 + 1) end)
        zero_gk = build_ring(n, h, fn v -> wrap64(v + 7) end)
        even_gk = build_ring(n, h, fn v -> wrap64(v * 101) end)
        odd_gk  = build_ring(n, h, fn v -> wrap64(v * 101 + 1) end)

        # prompt for input (recursive loop)
        input_loop(neg_gk, zero_gk, even_gk, odd_gk, 1)
    end

    def input_loop(neg_gk, zero_gk, even_gk, odd_gk, token_counter) do
        input = IO.gets("Enter a number: \n") |> String.trim()

        if input == "done" do
            # :done goes into each gatekeeper's mailbox after any queued :input messages,
            # so all in-flight and queued work finishes before shutdown
            send(neg_gk, :done)
            send(zero_gk, :done)
            send(even_gk, :done)
            send(odd_gk, :done)
            IO.puts("Shutting down")
        else
            case Integer.parse(input) do
                {num, _} ->
                    cond do
                        num < 0 ->
                            send(neg_gk, {:input, token_counter, :NEG, num})
                        num == 0 ->
                            send(zero_gk, {:input, token_counter, :ZERO, num})
                        num > 0 && rem(num, 2) == 0 ->
                            send(even_gk, {:input, token_counter, :POS_EVEN, num})
                        num > 0 && rem(num, 2) != 0 ->
                            send(odd_gk, {:input, token_counter, :POS_ODD, num})
                    end
                    input_loop(neg_gk, zero_gk, even_gk, odd_gk, token_counter + 1)

                :error ->
                    IO.puts("Error: '#{input}' is not a valid integer")
                    input_loop(neg_gk, zero_gk, even_gk, odd_gk, token_counter)
            end
        end
    end

    def build_ring(n, h, transform) do
        pids = Enum.map(1..n, fn _ -> spawn(FourRings, :node_init, [transform]) end)

        pids
        |> Enum.with_index()
        |> Enum.each(fn {pid, i} ->
            next = Enum.at(pids, rem(i + 1, n))
            send(pid, {:set_next, next})
        end)

        first = hd(pids)
        spawn(FourRings, :gatekeeper, [first, h])
    end

    # --- Gatekeeper ---

    # enforces one-token-at-a-time rule using nested receive blocks
    def gatekeeper(node_1, h) do
        receive do
            {:input, token_id, ring_id, value} ->
                send(node_1, {:token, token_id, ring_id, value, value, h, self()})

                # process is blocked until "complete" message comes through
                receive do
                    {:complete, ^token_id, ring_id, orig_input, final_val} ->
                        IO.puts("[Token #{token_id}, #{ring_id} Ring]: #{orig_input} -> #{final_val}")
                end
                gatekeeper(node_1, h)

            :done ->
                send(node_1, :done)
                :ok
        end
    end

    # --- Nodes ---

    def node_init(transform) do
        receive do
            {:set_next, next_pid} -> node_loop(next_pid, transform)
        end
    end

    def node_loop(next_pid, transform) do
        keep_running = receive do
            {:token, token_id, ring_id, orig_input, current_val, remaining_hops, gk} ->
                new_val = transform.(current_val)
                hops_left = remaining_hops - 1

                if hops_left > 0 do
                    send(next_pid, {:token, token_id, ring_id, orig_input, new_val, hops_left, gk})
                else
                    send(gk, {:complete, token_id, ring_id, orig_input, new_val})
                end
                true

            :done ->
                send(next_pid, :done)
                false
        end

        if keep_running do
            node_loop(next_pid, transform)
        else
            :ok
        end
    end
end
